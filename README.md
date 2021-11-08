# olca-simapro-csv

This is an API for writing and reading data sets in the SimaPro CSV format.
There is no formal specification of the format available but it is quite easy
to understand it when you export data sets from SimaPro and look into the files.

## SimaPro CSV data format
### File encoding
SimaPro is a Windows program, and thus we use [Windows-1252](https://en.wikipedia.org/wiki/Windows-1252)
as default file encoding for reading and writing. However, you can set the file encoding in the constructor
calls of the respective readers and writers.

### Header
Each dataset starts with a file header which looks like this:

```
{SimaPro 8.5.0.0}
{methods}
{Date: 2019-10-24}
{Time: 18:35:10}
{Project: Methods}
{CSV Format version: 8.0.5}
{CSV separator: Semicolon}
{Decimal separator: .}
{Date separator: -}
{Short date format: yyyy-MM-dd}
{Selection: Selection (1)}
{Related objects (system descriptions, substances, units, etc.): Yes}
{Include sub product stages and processes: No}
{Open library: 'Methods'}
```

This header starts with the first line and each header entry is enclosed in curly brackets.
Before you read the actual data from the file you need this information to parse the data
into the correct format.

### Blocks, sections and rows
After the header a SimaPro CSV file contains a set of blocks with data. Each data block starts
with a header and ends with the keyword `End`. For example the following is a block with quantity entries:

```
Quantities
Mass;Yes
Length;Yes
End
```

A block can contain data rows directly, like in the example above, or contain sections with data rows.
For example a process block starts with the header `Process` and contains a set of sections like `Category type`,
`Process identifier`, etc:

```
Process

Category type
material

Process identifier
DefaultX25250700002

Type
Unit process

...

End
```

As for the blocks each section has a header, however it does not end with the keyword `End` but with an empty line.
Data rows of a block or section are directly located in the next line under the header.
A section of a block starts with an empty line.

### Nuances
#### Boolean data type
Boolean type is represented in SimaPro CSV format as the following strings:
* `Yes` == `true`
* `No` == `false`

#### Numeric data type
In the different entities of the SimaPro CSV format, their `amount` attribute could be a `double` type, but also
it may be a formula. To handle this attribute, a `Numeric` class is provided in the API.

#### Flow identification
In SimaPro flows are identified by name. There is an unique constraint that checks for `Category` and `Name` combination.
This way, you can not create a product with the same name in the same Product stage category, for example.

Therefore, you can have a `Material` process with the same name as an `Assembly`. Then, both may be inputs to another `Assembly`
and you can export it to SimaPro CSV data format. However, this file cannot be imported to SimaPro because the program can not
identify what flow is the `Material` and what is the `Assembly`, as both have the same name.

## SimaPro CSV entities
### Reference data
#### System description block
A block including metadata related to the system:
```
System description

Name
system name

Category
Others

Description
text for description

Sub-systems
text for sub-systems

Cut-off rules
text for cut-off rules

Energy model
text for energy model

Transport model
text for transport model

Waste model
text for waste model

Other assumptions
text for other assumptions

Other information
text for other information

Allocation rules
text for allocation rules

End
```

#### Literature reference block
A block including metadata related to a literature reference:

```
Literature reference

Name
US EPA (1995)

Documentation link
http://www.epa.gov/ttnchie1/ap42/ch09/bgdocs/b9s13-2.pdf

Category
Others

Description
Test description

End
```

#### Quantity block and row
A block storing quantities (e.g.: flow properties in openLCA):
```
Quantities

Mass;Yes
Energy;Yes
Length;Yes

End
```

Each row in a Quantity block represents one Quantity. Their attributes are:

0. Name
1. Has dimension

#### Unit block and row
A block including the units:

```
Units

kg;Mass;1;kg
g;Mass;0,001;kg
kWh;Energy;3,6;MJ
MJ;Energy;1;MJ
ton;Mass;1000;kg
µg;Mass;0,000000001;kg
...

End
```

Each row in a Unit block represents one Unit. Their attributes are:

0. Name
1. Quantity
2. Conversion factor
3. Reference unit

#### Database input parameter block and row
A block including the input parameters in the database level:
```
Database Input parameters
db_input_param;1;Lognormal;1;0;0;No;database parameter

End
```

Each row is an input parameter with the following attributes:

0. name
1. value
2. uncertainty

Uncertainty goes from position 2 to 5, then:

6. is hidden
7. comment

#### Database calculated parameter block and row
A block including the calculated parameters in the database level:
```
Database Calculated parameters
db_calc_param;db_input_param * 3;calculated database parameter

End
```

Each row is a calculated parameter with the following attributes:

0. name
1. expression
2. comment

### Project input and calculated parameters
These are analogous to the previous, but in the project level:

```
Project Input parameters
proj_input_param;32;Uniform;0;10;35;No;project input parameter

End

Project Calculated parameters
proj_calc_param;db_input_param *4;project calculated parameter

End
```

### Elementary flow blocks and rows
Different blocks are presented for different elementary flow types:
```
Raw materials
Acids;kg;;

End


Airborne emissions
(+-)-Citronellol;kg;026489-01-0;

End


Waterborne emissions
(1r,4r)-(+)-Camphor;kg;000464-49-3;

End


Final waste flows
Asbestos;kg;;

End


Emissions to soil
1'-Acetoxysafrole;kg;034627-78-6;No formula available

End


Non material emissions
Noise from bus km;km;;

End


Social issues
venting of argon, crude, liquid;kg;;

End


Economic issues
Sample economic issue;kg;;

End
```

The SimaPro CSV format uses different names to identify these types in different contexts.
An enumeration is included in the API for reflecting this:

```java
public enum ElementaryFlowType {

    RESOURCES(
            "Resources", "Raw materials", "Raw"),

    EMISSIONS_TO_AIR(
            "Emissions to air", "Airborne emissions", "Air"),

    EMISSIONS_TO_WATER(
            "Emissions to water", "Waterborne emissions", "Water"),

    EMISSIONS_TO_SOIL(
            "Emissions to soil", "Emissions to soil", "Soil"),

    FINAL_WASTE_FLOWS(
            "Final waste flows", "Final waste flows", "Waste"),

    NON_MATERIAL_EMISSIONS(
            "Non material emissions", "Non material emissions", "Non mat."),

    SOCIAL_ISSUES(
            "Social issues", "Social issues", "Social"),

    ECONOMIC_ISSUES(
            "Economic issues", "Economic issues", "Economic");
}
```

Each row of one of these elementary flow blocks represent an elementary flow metadata with the following attributes:

0. name
1. unit
2. cas
3. comment

### Uncertainty record
An uncertainty distribution can be stored in 4 slots of a CSV row
(as in the previous input parameters examples). The first
slot contains the distribution type:

* Undefined
* Lognormal
* Normal
* Triangle
* Uniform

The other slots contain the distribution parameters.

### Process block
A process block has the following sections:
#### Simple sections
Sections with only one entry (no rows). In code block their corresponding
type in the API:

* Category type `ProcessCategory`
* Process identifier `String`
* Type `ProcessType`
* Process name `String`
* Status `String`
* Time period `String`
* Geography `String`
* Technology `String`
* Representativeness `String`
* Multiple output allocation `String`
* Substitution allocation `String`
* Cut off rules `String`
* Capital goods `String`
* Boundary with nature `String`
* Infrastructure `Boolean`
* Date `String`
* Record `String`
* Generator `String`
* Collection method `String`
* Data treatment `String`
* Verification `String`
* Comment `String`
* Allocation rules `String`

`ProcessCategory` and `ProcessType` are represented with the following enums:

```java
public enum ProcessCategory {

    MATERIAL("material"),

    ENERGY("energy"),

    TRANSPORT("transport"),

    PROCESSING("processing"),

    USE("use"),

    WASTE_SCENARIO("waste scenario"),

    WASTE_TREATMENT("waste treatment");
}
```

```java
public enum ProcessType {

  SYSTEM("System"),

  UNIT_PROCESS("Unit process");
```

#### Row sections
##### Literature references
```
Literature references
Ecoinvent 3;is copyright protected: false
```
Attributes:

0. name
1. comment

##### System description
```
System description
U.S. LCI Database;system description comment
```
Attributes:

0. name
1. comment

##### Products
```
Products
my product;kg;0,5;100;not defined;Agricultural;
```
Attributes:

0. name
1. unit
2. amount
3. allocation
4. waste type
5. category
6. comment

##### Technosphere exchanges
```
Avoided products
Wool, at field/US;kg;1;Undefined;0;0;0;

Materials/fuels
Soy oil, refined, at plant/kg/RNA;kg;0;Undefined;0;0;0;

Electricity/heat
Electricity, biomass, at power plant/US;kWh;0,1;Undefined;0;0;0;
```
Attributes:

0. name
1. unit
2. amount
3. uncertainty

From position 3 to 6, then:

7. comment

##### Elementary flow exchanges
```
Resources
Acids;;kg;1;Undefined;0;0;0;

Emissions to air
(+-)-Citronellol;low. pop.;kg;1;Lognormal;2;0;0;(1,2,3,4,5) with comment

Emissions to water
(1r,4r)-(+)-Camphor;lake;kg;1;Normal;3;0;0;

Emissions to soil
1'-Acetoxysafrole;forestry;kg;1;Triangle;0;1;5;
```
Attributes:

0. name
1. subcompartment
2. unit
3. amount
4. uncertainty

From position 4 to 7, then:

8. comment

##### Input and calculated parameters
Analogous to the explained rows in the Reference data section.

#### Waste treatment and waste scenario
TODO.

### Product stage block
A product stage block has the following sections:
#### Simple sections
Sections with only one entry (no rows). In code block their corresponding
type in the API:

* Category `ProductStageCategory`
* Status `String`

A `ProductStageCategory` is represented in the API with the following enum:
```java
public enum ProductStageCategory {

    ASSEMBLY("assembly"),
    DISASSEMBLY("disassembly"),
    DISPOSAL_SCENARIO("disposal scenario"),
    LIFE_CYCLE("life cycle"),
    REUSE("reuse");

}
```

#### Row sections
##### Products
```
Products
assembly;p;1;Others;;
```
Attributes:

0. name
1. unit
2. amount
3. category
4. comment

##### Technosphere exchanges
Analogous to `Processes`.

##### Input and calculated parameters
Analogous to `Processes`.

### Method block
An impact method block has the following sections:
#### Simple sections
Sections with only one entry (no rows). In code block their corresponding
type in the API:

* Name `String`
* Comment `String`
* Category `String`
* Use Damage Assessment `Boolean`
* Use Normalization `Boolean`
* Use Weighting `Boolean`
* Use Addition `Boolean`
* Weighting unit `String`

#### Row sections
##### Version
```
Version
1;431
```
Attributes:

0. major
1. minor

##### Impact category
```
Impact category
Climate change, ecosystem quality, short term;PDF.m2.yr
```
Attributes:

0. name
1. unit

Each impact category has a `Substances` section with the characterization factors:
```
Substances
Air;(unspecified);(E)-HFC-1225ye;10/8/5595;0;kg
Air;(unspecified);(E)-HFC-1234ze;1645-83-6;0.177;kg
```
Each row has the following attributes:

0. compartment
1. subcompartment
2. flow
3. cas number
4. factor
5. unit

##### Damage category
```
Damage category
Ecosystem quality;PDF.m2.yr
```
Attributes:

0. name
1. unit

Each damage category has a `Impact categories` section with the damage factors:
```
Impact categories
Climate change, ecosystem quality, long term;1.00E+00
Climate change, ecosystem quality, short term;1.00E+00
```
Each row has the following attributes:

0. impact category
1. factor

## Normalization-Weighting set
```
Normalization-Weighting set
IMPACT World+ (Stepwise 2006 values)
```
Attributes:

0. name

Each set has a `Normalization` and a `Weighting` sections:
```
Normalization
Human health;1.37E+01
Ecosystem quality;1.01E-04

Weighting
Human health;5401.459854
Ecosystem quality;1386.138614
```
Attributes:

0. impact category
1. factor

## Reading datasets
### CSV Reader
A reader can be initialized with the `CSV.readerOf()` static method. This method has the following overloads:
```java
CSV.readerOf(File file)
```
```java
CSV.readerOf(File file, Charset charset)
```
```java
CSV.readerOf(InputStream stream, Charset charset)
```
If a charset is not specified, the default Windows-1252 is used.

### Header
As explained before, previous to read the actual data from the file you need the header information
to parse the data into the correct format. Thus, there is a `CsvHeader` which just reads this information
from a file:

```java
var stream = getClass().getResourceAsStream("header.csv");
var reader = Csv.readerOf(stream, StandardCharsets.UTF_8));
var header = CsvHeader.readFrom(reader);
```

### CsvDataSet
It is the main class for reading datasets in SimaPro CSV format. It can be used as follows:
```java
var dataset = CsvDataSet.read(header, reader)
```
Once the `dataset` object is instantiated, it contains all the SimaPro entities present in the file.
For example, `dataset.processes()` has an array with all the processes. Then process attributes can be
accessed:
```java
var process = dataset.processes().get(0);
var name = process.name();
var product = process.products.get(0);
var productAmount = product.amount();
```
As this example, the API provides support for all the SimaPro entities detailed before.

Finally, if you want to write the data:
```java
dataset.write(file);
```
## References

https://github.com/laurepatou/IMPACT-World-in-Simapro

https://github.com/massimopizzol/Simapro-CSV-converter

* `;;;;;`

## TODO:
* are sub-compartments really a close list? -> test them
* support new format fields: `PlatformId`, UUIDs?
* what is the 4th column of a WasteFractionRow?
* what are the separators for function parameters in formulas?
* parse date-fields into type `Date`
* remove the Pedigree fields and the Pedigree matcher; this should go into the
  openLCA import then
* header: check which separators could occur
* header: add a write-method, and write necessary defaults
* need a clean definition how we handle decimal separators and how this
  works with function parameter separators
* make it `null` friendly: **no** getter should **ever** return `null` (use
  `Optional` or default values like `""` or `0` ) but accept
  `null` in the setters
* status -> enum: `null`, `Temporary`, `Draft`, `To be revised`, `To be reviewed` and `Finished`
