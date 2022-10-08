# twbxless

*twbxless* makes data exported in Tableau packaged workbook files (.twbx) available at CSV URLs for other tools,
e.g. Excel data models or Google Sheets IMPORTDATA. This is possible thanks to Tableau's
[Hyper API](https://help.tableau.com/current/api/hyper_api/en-us/index.html).

It's possibly also useful for sustaining a community of Tableau visualization makers, where new workbooks are made from data available only through other workbooks/twbxless. Using twbxless as a data source in Tableau may require [a CSV web data connector](https://help.tableau.com/current/pro/desktop/en-us/examples_web_data_connector.htm).

[Let us know how we can make twbxless better](/../../issues)!

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

First you'll need to determine the URL of a Tableau workbook (.twbx file) published on the web.

For example, [for this workbook featured on Viz of the Day](https://public.tableau.com/profile/maximiliano4575#!/vizhome/FemaleDirectors/FemaleDirectors)
the `url` needed is the one the browser navigates to (downloading the file) when you click Tableau Public's "Download" button: <https://public.tableau.com/workbooks/FemaleDirectors.twb>.

Compose that `url` together with twbxless' `/datasources` URL to list the data source extracts in the workbook:

```
http://localhost:8080/datasources?url=https://public.tableau.com/workbooks/FemaleDirectors.twb
```

You get a list of data source names (and corresponding extract filenames) within the workbook, in CSV format (yeah, only one column):

```
name
Hoja1 (genderOverall)
```

This tells us there's just 1 data source in *FemaleDirectors.twb*, named *Hoja1 (genderOverall)*.

Then use twbxless' `/data` URL, with the same `url` param/value, adding `name`:

```
http://localhost:8080/data?url=https://public.tableau.com/workbooks/FemaleDirectors.twb&name=Hoja1 (genderOverall)
```

You get back the row/column data from that data source, in CSV format:

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

URLs like this last `/data` URL could be used in any tool that works with CSV URLs, for example Excel's "Get Data From Web", or even Tableau itself if you have a CSV data connector.

## Frequently asked questions

### Does twbxless provide access to the external data sources used to make a workbook?

No, twbxless only reads data that's extracted, stored within .twbx files.

### How often can I get fresh data?

Because twbxless only reads the data extracted into the .twbx file, the publisher of the workbook would need to refresh the extract first using Tableau. For example see [How to Keep Data Fresh](https://help.tableau.com/current/online/en-us/to_keep_data_fresh.htm).

### Does Google Sheets' IMPORTDATA work with http://localhost:8080 URLs like the ones in the examples?

No, unfortunately Google Sheets IMPORTDATA URLs need to be accessible from the internet. Run twbxless from a cloud container host,
e.g. Google Cloud Run, Azure Containers, or AWS ECS, instead of on your computer, if you need to use it with Google Sheets.

## Limitations

- twbxless doesn't support all column types, for example geography. Unsupported columns will appear in the `/data` response, but
non-null values in those columns appear as `TYPE?`. Please [file an issue with an example workbook](/../../issues) if you'd like
support for a particular type.
- twbxless supports a single schema & table per .hyper file. I've seen plenty of workbooks with multiple .hyper files (one per
data source), but never a workbook where there was more than one schema/table in the file. If you need this
[please provide an example workbook](/../../issues/4).

## Configuration (optional)

twbxless supports 3 configuration environment variables

> **URLPREFIX**: required prefix for any URL retrieved (default is `https://public.tableau.com/`)
>
> **HYPEREXEC**: path to the executables (e.g. hyperd or hyperd.exe) packaged with Hyper API (default is `/hyperapi/lib/hyper` since that's where [Dockerfile](Dockerfile) puts them)
>
> **PORT**: the port to bind to (default is `8080`)

## Building/running outside container (optional, not recommended)

If you want to dev/build/run outside a container, here are partial steps e.g. for Windows:

- provide JDK 14 and gradle
- [download Hyper API](https://tableau.com/support/releases/hyper-api/latest)
- extract *lib* from the Hyper API package and put it along side *src*
- `gradle build`
- `set HYPEREXEC=lib\hyper`
- `java -jar build/libs/twbxless.jar`
