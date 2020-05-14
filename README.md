# hypersuck
hypersuck makes data exported into Tableau packaged workbooks available at CSV URLs for use in other tools, 
e.g. Excel data models or Google Sheets IMPORTDATA. This is possible thanks to 
[Tableau's Hyper API](https://help.tableau.com/current/api/hyper_api/en-us/index.html).

It's possibly also useful for creating a network of Tableau workbooks, where creators riff off data 
published by another organization in the Tableau format without worrying about the details of the workbook aside
from its data. (This might require 
[web data connector](https://help.tableau.com/current/pro/desktop/en-us/examples_web_data_connector.htm) 
or Google Sheets IMPORTDATA, as it doesn't seem Tableau can pull CSV URLs directly.)

## Build

For Docker, provide docker then `docker build . -t hypersuck`.

On Windows (if you don't want to use Docker for Windows), provide Java 11 and gradle, then 
[download Hyper API](https://tableau.com/support/releases/hyper-api/latest) and extract *lib* directory (which 
contains e.g. *tableauhyperapi.jar* and *hyper/hyperd.exe*) to *lib* alongside *src*.

Then `gradle build`.

## Run

For Docker, after building:

```
docker run -p8080:8080 hypersuck:latest
```

**HYPEREXEC**: full path to the executables packaged with Hyper API. Defaults to `/hyperapi/lib/hyper` since that's 
correct for the provided Dockerfile.

**PORT**: the port to bind HTTP listener to. Defaults to `8080`.

For Windows, you're on your own. It'll be something like:

```
java -jar build/libs/hypersuck-0.0.2.jar -DHYPEREXEC=lib\hyper
```   

## Use

First you'll need to identify the .hyper extracts within a .twbx that's published on the web. To do that, 
use the `/filenames` endpoint, specifying the `url` that downloads the workbook in .twbx format.

For example, for [this workbook featured on Viz of the Day](https://public.tableau.com/profile/maximiliano4575#!/vizhome/FemaleDirectors/FemaleDirectors)
, the`url` we need is the one under the "Download" button. Use this with the `/filenames` endpoint:

```
http://localhost:8080/filenames?url=https://public.tableau.com/workbooks/FemaleDirectors.twb
```

and in CSV format you get the filenames (just 1 filename):

```
filenames
Data/Fuentes de datos/Hoja1 (genderOverall).hyper
```

Then, we use the `/data` endpoint with the same `url` and the `filename` we want:

```
http://localhost:8080/data?url=https://public.tableau.com/workbooks/FemaleDirectors.twb&filename=Data/Fuentes de datos/Hoja1 (genderOverall).hyper
```

and this returns that data set (snipped)

```
"genre","year","gender","freq","percent","filter"
Total,2000,female,17,0.096,0
Total,2000,male,161,0.904,0
Total,2001,female,15,0.075,0
Total,2001,male,186,0.925,0
Total,2002,female,15,0.069,0
Total,2002,male,201,0.931,0
Total,2003,female,16,0.076,0
... <snipped here> ...
```

## Limitations

- Not all column types are supported. Geography, for example, isn't supported. If an unsupported columns's detected the
  column's included in the CSV, but non-null values will be `TYPE?`.
- .hyper files can contain multiple schemas, and multiple tables within those schemas, but I didn't have any workbooks
  where that was the case. If used with such a workbook `/data` states this rather than choosing an arbitrary table. 
  Show an example workbook in [Issues](Issues) and we can consider adding a `table` parameter.

## FAQ

##### Does this provide access to the external data sources used to make a workbook?

No. It only reads data that's been extracted into the .hyper files within the .twbx file.
(It's similar to what "View Data..." under "Data" in Tableau Desktop can show when completely disconnected from the 
external source.)

##### I can't get =IMPORTDATA("http<nolink>://localhost:8080/data?url=whatever&filename=whatever") to work

Google Sheets needs the service to be accessible from the internet. I suggest running the container on some serverless
something, e.g. Google Cloud Run, Azure, Amazon, Heroku.


