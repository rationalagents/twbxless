# twbxless
*twbxless* makes data exported in Tableau packaged workbook files (.twbx) available at CSV URLs for other tools, 
e.g. Excel data models or Google Sheets IMPORTDATA. This is possible thanks to 
[Tableau's Hyper API](https://help.tableau.com/current/api/hyper_api/en-us/index.html).

It's possibly also useful for sustaining a community of Tableau visualization builders, where creators make new 
public workbooks from data exported to other public workbooks, rather than having to copy those workbooks and deal 
with new copies to get new data. Using twbxless this way may require
[web data connector](https://help.tableau.com/current/pro/desktop/en-us/examples_web_data_connector.htm) 
or Google Sheets IMPORTDATA, as it doesn't seem Tableau, as of 2020.2, can fetch CSV data from the web on its own.

## Build

To build twbxless as a container, provide 
[Docker](https://hub.docker.com/search?q=&type=edition&offering=community&sort=updated_at&order=desc),
then `docker build . -t twbxless`.

If you want to dev/build outside a container,
 - provide JDK 14 and gradle
 - [download Hyper API](https://tableau.com/support/releases/hyper-api/latest)
 - extract *lib* from the Hyper API package and put it along side *src*
 - `gradle build`

## Run

After building the container,

```
docker run -it -p8080:8080 twbxless:latest
```

and you should see output like:

```
2020-05-14 23:39:50.695  INFO 1 --- [main] com.rationalagents.twbxless.Application     : Starting Application
```

That means it's running, and you're ready to move onto **Use.** You can Ctrl+C to stop.

If you're thinking "yeah, I already did hard way once, `gradle build`, it definitely built, let me keep on this 
so-called hard way, and I'm on Windows, so whatcha got for me dawg", do something like this:

```
set HYPEREXEC=lib\hyper
java -jar build/libs/twbxless.jar
```

## Config (optional)

twbxless supports 3 configuration environment variables:

**PORT**: lingua franca in servlerless, the port to bind to (default is `8080`)

**HYPEREXEC**: path to the executables (e.g. hyperd or hyperd.exe) packaged with Hyper API (default is `/hyperapi/lib/hyper` since that's where [Dockerfile](Dockerfile) puts them)

**URLPREFIX**: required prefix for any URL retrieved (default is `https://public.tableau.com/`)


## Use

First you'll need to identify the data extracts within a .twbx that's published on the web. To do that, 
use the `/filenames` endpoint, specifying the `url` to the workbook.

For example, for [this workbook featured on Viz of the Day](https://public.tableau.com/profile/maximiliano4575#!/vizhome/FemaleDirectors/FemaleDirectors)
, the `url` we need's the one backing Tableau Public's "Download" button. Use `/filenames` with that `url`: 

```
http://localhost:8080/filenames?url=https://public.tableau.com/workbooks/FemaleDirectors.twb
```

and in CSV format you get a list of filenames (there's just 1 filename for *FemaleDirectors.twb*):

```
filenames
Data/Fuentes de datos/Hoja1 (genderOverall).hyper
```

Then we switch to the `/data` endpoint, using the same `url`, adding `filename`:

```
http://localhost:8080/data?url=https://public.tableau.com/workbooks/FemaleDirectors.twb&filename=Data/Fuentes de datos/Hoja1 (genderOverall).hyper
```

and we get the data from that file

```
"genre","year","gender","freq","percent","filter"
Total,2000,female,17,0.096,0
Total,2000,male,161,0.904,0
Total,2001,female,15,0.075,0
Total,2001,male,186,0.925,0
Total,2002,female,15,0.069,0
Total,2002,male,201,0.931,0
Total,2003,female,16,0.076,0
...
```

## Limitations

- This doesn't support all column types, for example geography. It'll include unsupported columns in the CSV, but 
non-null values will be `TYPE?`. Please provide an example workbook at [issues](/../../issues) if you'd like support
for a particular type.
- Only a single schema & table per file are supported. [Let us know if you'd like support](/../../issues/4).

## FAQ

#### Does twbxless provide access to the external data sources used to make a workbook?

No. It only reads the data that's directly in the .twbx file.

#### Does Google Sheets' IMPORTDATA work with http:<nolink>//localhost:8080 URLs?

No, Google Sheets needs twbxless to be accessible from the internet. Run twbxless from some serverless somewhere,
e.g. Google Cloud Run, Azure Containers, AWS, or Heroku, instead of on your computer.


