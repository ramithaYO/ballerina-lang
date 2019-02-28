public type PackageParser object {
    BirChannelReader reader;
    TypeParser typeParser;

    public function __init(BirChannelReader reader, TypeParser typeParser) {
        self.reader = reader;
        self.typeParser = typeParser;
    }

    public function parseVariableDcl() returns VariableDcl {
        var kind = self.parseVarKind();
        VariableDcl dcl = {
            typeValue: self.reader.readBType(),
            name: { value: self.reader.readStringCpRef() },
            kind: kind
        };
        return dcl;
    }

    public function parseFunction() returns Function {
        var name = self.reader.readStringCpRef();
        var isDeclaration = self.reader.readBoolean();
        var visibility = parseVisibility(self.reader);
        var typeTag = self.reader.readInt8();
        if (typeTag != self.typeParser.TYPE_TAG_INVOKABL_TYPE) {
            error err = error("Illegal function signature type tag" + typeTag);
            panic err;
        }
        var sig = self.typeParser.parseInvokableType();
        var argsCount = self.reader.readInt32();
        var numLocalVars = self.reader.readInt32();

        VariableDcl[] dcls = [];
        map<VariableDcl> localVarMap = {
        
        };
        int i = 0;
        while (i < numLocalVars) {
            var dcl = self.parseVariableDcl();
            dcls[i] = dcl;
            localVarMap[dcl.name.value] = dcl;
            i += 1;
        }
        FuncBodyParser bodyParser = new(self.reader, localVarMap);

        BasicBlock[] basicBlocks = [];
        var numBB = self.reader.readInt32();
        i = 0;
        while (i < numBB) {
            basicBlocks[i] = bodyParser.parseBB();
            i += 1;
        }

        return {
            name: { value: name },
            isDeclaration: isDeclaration,
            visibility: visibility,
            localVars: dcls,
            basicBlocks: basicBlocks,
            argsCount: argsCount,
            typeValue: sig
        };
    }

    public function parsePackage() returns Package {
        var pkgIdCp = self.reader.readInt32();
        ImportModule[] importModules = self.parseImportMods();
        TypeDef[] typeDefs = self.parseTypeDefs();
        var numFuncs = self.reader.readInt32();
        Function[] funcs = [];
        int i = 0;
        while (i < numFuncs) {
            funcs[i] = self.parseFunction();
            i += 1;
        }
        //BirEmitter emitter = new({ typeDefs: typeDefs, functions: funcs });
        //emitter.emitPackage();
        return { importModules: importModules, typeDefs: typeDefs, functions: funcs };
    }

    function parseImportMods() returns ImportModule[] {
        int numImportMods = self.reader.readInt32();
        ImportModule[] importModules = [];
        foreach var i in 0..<numImportMods {
            string modOrg = self.reader.readStringCpRef();
            string modName = self.reader.readStringCpRef();
            string modVersion = self.reader.readStringCpRef();
            importModules[i] = { modOrg: { value: modOrg }, modName: { value: modName },
                modVersion: { value: modVersion } };
        }
        return importModules;
    }

    function parseTypeDefs() returns TypeDef[] {
        int numTypeDefs = self.reader.readInt32();
        TypeDef[] typeDefs = [];
        int i = 0;
        while i < numTypeDefs {
            typeDefs[i] = self.parseTypeDef();
            i = i + 1;
        }
        return typeDefs;
    }

    function parseTypeDef() returns TypeDef {
        string name = self.reader.readStringCpRef();
        Visibility visibility = parseVisibility(self.reader);
        return { name:{ value: name}, visibility: visibility, typeValue: self.typeParser.parseType()};
    }

    public function parseVarKind() returns VarKind {
        int b = self.reader.readInt8();
        if (b == 1) {
            return "LOCAL";
        } else if (b == 2) {
            return "ARG";
        } else if (b == 3) {
            return "TEMP";
        } else if (b == 4) {
            return "RETURN";
        }
        error err = error("unknown var kind tag " + b);
        panic err;
    }

    public function parseSig(string sig) returns BInvokableType {
        BType returnType = "int";
        //TODO: add boolean
        if (sig.lastIndexOf("(N)") == (sig.length() - 3)) {
            returnType = "()";
        }
        return {
            retType: returnType
        };
    }

};

public function parseVisibility(BirChannelReader reader) returns Visibility {
    int b = reader.readInt8();
    if (b == 0) {
        return "PACKAGE_PRIVATE";
    } else if (b == 1) {
        return "PRIVATE";
    } else if (b == 2) {
        return "PUBLIC";
    }
    error err = error("unknown variable visiblity tag " + b);
        panic err;
}

