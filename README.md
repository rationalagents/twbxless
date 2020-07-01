# twbxless

*twbxless* makes data exported in Tableau packaged workbook files (.twbx) available at CSV URLs for other tools,
e.g. Excel data models or Google Sheets IMPORTDATA. This is possible thanks to
[Tableau's Hyper API](https://help.tableau.com/current/api/hyper_api/en-us/index.html).

It's possibly also useful for sustaining a community of Tableau visualization riffs, where creators make new
public workbooks from data exported to other workbooks, rather than having to copy those workbooks and deal
with new copies to get new data. [Let us know how we can improve it!](/../../issues) Using twbxless for
that may require [a CSV web data connector](https://help.tableau.com/current/pro/desktop/en-us/examples_web_data_connector.htm).

## Build

To build twbxless as a container, provide
[Docker](https://hub.docker.com/search?q=&type=edition&offering=community&sort=updated_at&order=desc)
then run

```
docker build . -t twbxless
```

## Run

After building, run

```
docker run -it -p8080:8080 twbxless:latest
```

You should see

```
2020-05-14 23:39:50.695  INFO 1 --- [main] com.rationalagents.twbxless.Application     : Starting Application
```

If so, you're ready to move on to **Use.**

Ctrl+C will stop the container once you're done with it.

## Use

First you'll need to identify the data extracts within a .twbx that's published on the web. To do that,
use the `/filenames` endpoint, specifying the `url` to the workbook.

For example, for [this workbook featured on Viz of the Day](https://public.tableau.com/profile/maximiliano4575#!/vizhome/FemaleDirectors/FemaleDirectors)
, the `url` we need's the one backing Tableau Public's "Download" button. Use `/filenames` with that `url`:

```
http://localhost:8080/filenames?url=https://public.tableau.com/workbooks/FemaleDirectors.twb
```

and in CSV format you get a list of .hyper extract filenames within that workbook (there's just 1 in *FemaleDirectors.twb*):

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
genre,year,gender,freq,percent,filter
Total,2000,female,17,0.096,0
Total,2000,male,161,0.904,0
Total,2001,female,15,0.075,0
Total,2001,male,186,0.925,0
Total,2002,female,15,0.069,0
Total,2002,male,201,0.931,0
Total,2003,female,16,0.076,0
...
```
## FAQ

### Does twbxless provide access to the external data sources used to make a workbook?

No. It only reads the data that's directly in the .twbx file.

### Does Google Sheets' IMPORTDATA work with http:<nolink>//localhost:8080 URLs?

No, Google Sheets IMPORTDATA needs twbxless to be accessible from the internet. Run twbxless from some serverless somewhere,
e.g. Google Cloud Run, Azure Containers, AWS, or Heroku, instead of on your computer.

## Limitations

- This doesn't support all column types, for example geography. Any unsupported column will appear in the CSV, but
non-null values in the column will be `TYPE?`. Please [file an issue with an example workbook](/../../issues) if you'd like support
for a particular type.
- Only supports single schema & table per .hyper file. I've seen plenty of workbooks with multiple .hyper files (one per
data source), but never a workbook where there was >1 schema/table in a file.  If you need this
[please provide an example workbook](/../../issues/4).

## Configuring twbxless (optional)

twbxless supports 3 configuration environment variables.

**URLPREFIX**: required prefix for any URL retrieved (default is `https://public.tableau.com/`)

**HYPEREXEC**: path to the executables (e.g. hyperd or hyperd.exe) packaged with Hyper API (default is `/hyperapi/lib/hyper` since that's where [Dockerfile](Dockerfile) puts them)

**PORT**: the port to bind to (default is `8080`)

## Building/running outside container (optional, not recommended)

If you want to dev/build/run outside a container, here are partial steps e.g. for Windows:

- provide JDK 14 and gradle
- [download Hyper API](https://tableau.com/support/releases/hyper-api/latest)
- extract *lib* from the Hyper API package and put it along side *src*
- `gradle build`
- `set HYPEREXEC=lib\hyper`
- `java -jar build/libs/twbxless.jar`
