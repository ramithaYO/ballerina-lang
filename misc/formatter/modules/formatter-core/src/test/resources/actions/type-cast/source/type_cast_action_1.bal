type Person record {
   string name;
};

type Employee record {
   string name;
};

public function foo() {
   Employee employee = {name: "Jane Doe"};
   Person2 person  =   <  Person2  >
     employee   ;
   anydata value = 100;
   int   i   =   <int  >   value
   ;
   float   f   =    <  float  >  value   ;
   float  |  boolean   u   =   <   float  |   boolean  >
   value;
}
