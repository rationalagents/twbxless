# hypersuck
hypersuck's a service that retrieves Tableau workbook (.twbx) that've been published to the web, providing access to the
raw data as CSV.

This is useful for building dashboards/visualizations using other tools, e.g. Google Data Studio or Power BI, using
what was published first in Tableau as a basis.

This is possible thanks to [Tableau's Hyper API](https://help.tableau.com/current/api/hyper_api/en-us/index.html),
which allows queries against Tableau Hyper extracts.

## Build

On Windows, provide Java 11+ & gradle, then [download Hyper API](https://tableau.com/support/releases/hyper-api/latest) 
and extract its *lib* directory (which contains e.g. *tableauhyperapi.jar* and *hyper/hyperd.exe*) to `lib` 
alongside `src`.

Then `gradle build`.

For Linux/container, provide docker then `docker build . -t hypersuck`. This builds off *openjdk:11*, downloading
Hyper API 0.0.10622 and everything else needed to build & run hypersuck.

## Run

For Linux/container, after building:

```
docker run -p8080:8080 hypersuck:latest
```

**HYPEREXEC**: full path to the executables packaged with Hyper API. Defaults to `/hyperapi/lib/hyper` under 
assumption you'll use the container.

**PORT**: the port to bind HTTP listener to. Defaults to `8080`.

## Use

List .hyper filenames within a .twbx (redirected from a .twb) as CSV:

```
http://localhost:8080/filenames?url=https://public.tableau.com/workbooks/DPHIdahoCOVID-19Dashboard_V2.twb
```

Then get the row data as CSV, given a filename:

```
http://localhost:8080/data?url=https://public.tableau.com/workbooks/DPHIdahoCOVID-19Dashboard_V2.twb&filename=County%20(COVID%20State%20Dashboard.V1).hyper
```

## Deploy

Although I'll do CI eventually, I can manually version and push a `docker build` result to GCR as follows:

```
docker tag hypersuck:latest us.gcr.io/hypersuck/hypersuck:v2
docker push us.gcr.io/hypersuck/hypersuck:v2
```

