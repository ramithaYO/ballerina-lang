/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.ballerinalang.net.http.nativeimpl;

import org.ballerinalang.jvm.scheduling.Scheduler;
import org.ballerinalang.jvm.values.ErrorValue;
import org.ballerinalang.jvm.values.ObjectValue;
import org.ballerinalang.jvm.values.connector.NonBlockingCallback;
import org.ballerinalang.mime.nativeimpl.MimeDataSourceBuilder;
import org.ballerinalang.mime.nativeimpl.MimeEntityBody;
import org.ballerinalang.mime.util.EntityBodyChannel;
import org.ballerinalang.mime.util.EntityBodyHandler;
import org.ballerinalang.mime.util.EntityWrapper;
import org.ballerinalang.mime.util.MimeUtil;
import org.ballerinalang.net.http.HttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.transport.http.netty.message.FullHttpMessageListener;
import org.wso2.transport.http.netty.message.HttpCarbonMessage;
import org.wso2.transport.http.netty.message.HttpMessageDataStreamer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import static org.ballerinalang.mime.util.EntityBodyHandler.constructBlobDataSource;
import static org.ballerinalang.mime.util.EntityBodyHandler.constructJsonDataSource;
import static org.ballerinalang.mime.util.EntityBodyHandler.constructStringDataSource;
import static org.ballerinalang.mime.util.EntityBodyHandler.constructXmlDataSource;
import static org.ballerinalang.mime.util.EntityBodyHandler.isStreamingRequired;
import static org.ballerinalang.mime.util.MimeConstants.ENTITY_BYTE_CHANNEL;
import static org.ballerinalang.mime.util.MimeConstants.NO_CONTENT_ERROR;
import static org.ballerinalang.mime.util.MimeConstants.PARSER_ERROR;
import static org.ballerinalang.mime.util.MimeConstants.TRANSPORT_MESSAGE;

/**
 * A wrapper class to handle http protocol related functionality before the data source build.
 *
 * @since slp3
 */
public class ExternHttpDataSourceBuilder extends MimeDataSourceBuilder {

    private static final Logger log = LoggerFactory.getLogger(ExternHttpDataSourceBuilder.class);

    public static Object getNonBlockingByteArray(ObjectValue entityObj) {
        Object transportMessage = entityObj.getNativeData(TRANSPORT_MESSAGE);
        if (isStreamingRequired(entityObj) || transportMessage == null) {
            return getByteArray(entityObj);
        }

        // access payload in non blocking manner
        NonBlockingCallback callback = null;
        try {
            Object messageDataSource = EntityBodyHandler.getMessageDataSource(entityObj);
            if (messageDataSource != null) {
                return getAlreadyBuiltByteArray(entityObj, messageDataSource);
            }
            callback = new NonBlockingCallback(Scheduler.getStrand());
            constructNonBlockingDataSource(callback, entityObj, SourceType.BLOB);
        } catch (Exception exception) {
            notifyError(callback, exception, "blob");
        }
        return null;
    }

    public static Object getNonBlockingJson(ObjectValue entityObj) {
        if (isStreamingRequired(entityObj)) {
            return getJson(entityObj);
        }

        // access payload in non blocking manner
        NonBlockingCallback callback = null;
        try {
            Object dataSource = EntityBodyHandler.getMessageDataSource(entityObj);
            if (dataSource != null) {
                return getAlreadyBuiltJson(dataSource);
            }
            callback = new NonBlockingCallback(Scheduler.getStrand());
            constructNonBlockingDataSource(callback, entityObj, SourceType.JSON);
        } catch (Exception exception) {
            notifyError(callback, exception, "json");
        }
        return null;
    }

    public static Object getNonBlockingText(ObjectValue entityObj) {
        if (isStreamingRequired(entityObj)) {
            return getText(entityObj);
        }

        // access payload in non blocking manner
        NonBlockingCallback callback = null;
        try {
            Object dataSource = EntityBodyHandler.getMessageDataSource(entityObj);
            if (dataSource != null) {
                return org.ballerinalang.jvm.StringUtils.fromString(MimeUtil.getMessageAsString(dataSource));
            }
            callback = new NonBlockingCallback(Scheduler.getStrand());
            constructNonBlockingDataSource(callback, entityObj, SourceType.TEXT);
        } catch (Exception exception) {
            notifyError(callback, exception, "text");
        }
        return null;
    }

    public static Object getNonBlockingXml(ObjectValue entityObj) {
        if (isStreamingRequired(entityObj)) {
            return getXml(entityObj);
        }

        // access payload in non blocking manner
        NonBlockingCallback callback = null;
        try {
            Object dataSource = EntityBodyHandler.getMessageDataSource(entityObj);
            if (dataSource != null) {
                return getAlreadyBuiltXml(dataSource);
            }

            callback = new NonBlockingCallback(Scheduler.getStrand());
            constructNonBlockingDataSource(callback, entityObj, SourceType.XML);
        } catch (Exception exception) {
            notifyError(callback, exception, "xml");
        }
        return null;
    }

    public static Object getByteChannel(ObjectValue entityObj) {
        HttpCarbonMessage httpCarbonMessage = (HttpCarbonMessage) entityObj.getNativeData(TRANSPORT_MESSAGE);
        if (httpCarbonMessage != null) {
            HttpMessageDataStreamer httpMessageDataStreamer = new HttpMessageDataStreamer(httpCarbonMessage);

            long contentLength = HttpUtil.extractContentLength(httpCarbonMessage);
            if (contentLength > 0) {
                entityObj.addNativeData(ENTITY_BYTE_CHANNEL, new EntityWrapper(
                        new EntityBodyChannel(httpMessageDataStreamer.getInputStream())));
            }
        }
        return MimeEntityBody.getByteChannel(entityObj);
    }

    public static void constructNonBlockingDataSource(NonBlockingCallback callback, ObjectValue entity,
                                                      SourceType sourceType) {
        HttpCarbonMessage inboundMessage = extractTransportMessageFromEntity(entity);
        inboundMessage.getFullHttpCarbonMessage().addListener(new FullHttpMessageListener() {
            @Override
            public void onComplete(HttpCarbonMessage inboundMessage) {
                Object dataSource = null;
                HttpMessageDataStreamer dataStreamer = new HttpMessageDataStreamer(inboundMessage);
                InputStream inputStream = dataStreamer.getInputStream();
                try {
                    switch (sourceType) {
                        case JSON:
                            dataSource = constructJsonDataSource(entity, inputStream);
                            updateJsonDataSourceAndNotify(callback, entity, dataSource);
                            return;
                        case TEXT:
                            dataSource = constructStringDataSource(entity, inputStream);
                            break;
                        case XML:
                            dataSource = constructXmlDataSource(entity, inputStream);
                            break;
                        case BLOB:
                            dataSource = constructBlobDataSource(inputStream);
                            break;
                    }
                    updateDataSourceAndNotify(callback, entity, dataSource);
                } catch (Exception e) {
                    createErrorAndNotify(callback, "Error occurred while extracting " +
                            sourceType.toString().toLowerCase(Locale.ENGLISH) + " data from entity: " + getErrorMsg(e));
                } finally {
                    try {
                        inputStream.close();
                    } catch (IOException exception) {
                        log.error("Error occurred while closing the inbound data stream", exception);
                    }
                }
            }

            @Override
            public void onError(Exception ex) {
                createErrorAndNotify(callback, "Error occurred while extracting content from message : " +
                        ex.getMessage());
            }
        });
    }

    private static void notifyError(NonBlockingCallback callback, Exception exception, String type) {
        ErrorValue error = (ErrorValue) createError(exception, type);
        setReturnValuesAndNotify(callback, error);
    }

    private static void createErrorAndNotify(NonBlockingCallback callback, String errMsg) {
        ErrorValue error = MimeUtil.createError(PARSER_ERROR, errMsg);
        setReturnValuesAndNotify(callback, error);
    }

    private static void setReturnValuesAndNotify(NonBlockingCallback callback, Object result) {
        callback.setReturnValues(result);
        callback.notifySuccess();
    }

    private static void updateDataSourceAndNotify(NonBlockingCallback callback, ObjectValue entityObj,
                                                  Object result) {
        updateDataSource(entityObj, result);
        setReturnValuesAndNotify(callback, result);
    }

    private static void updateJsonDataSourceAndNotify(NonBlockingCallback callback, ObjectValue entityObj,
                                                      Object result) {
        updateJsonDataSource(entityObj, result);
        setReturnValuesAndNotify(callback, result);
    }

    private static HttpCarbonMessage extractTransportMessageFromEntity(ObjectValue entityObj) {
        HttpCarbonMessage message = (HttpCarbonMessage) entityObj.getNativeData(TRANSPORT_MESSAGE);
        if (message != null) {
            return message;
        }
        throw MimeUtil.createError(NO_CONTENT_ERROR, "Empty content");
    }

    /**
     * Type of content to construct the data source.
     */
    public enum SourceType {
        JSON,
        XML,
        TEXT,
        BLOB
    }
}
