# hypersuck
hypersuck's a service that retrieves Tableau workbook (.twbx) that've been published to the web, providing access to the
raw data as CSV.

This is useful for building dashboards/visualizations using other tools, e.g. Google Data Studio or Power BI, using
what was published first in Tableau as a basis.

This is possible thanks to [Tableau's Hyper API](https://help.tableau.com/current/api/hyper_api/en-us/index.html),
which allows queries against Tableau Hyper extracts.

## Build

On Windows, provide Java 11+ & gradle, then [download Hyper API](https://tableau.com/support/releases/hyper-api/latest) 
and extract the *lib* directory (which contains jars, hyperd.exe) to `lib` alongside `src'. 

Then `gradle build`.

For Linux/container, provide docker then `docker build . -t hypersuck`. This builds an image `hypersuck:latest` that
includes Hyper API 0.0.10622 (happened to be the latest version at the time; I had no problems with it.)

## Run

For Linux/container, after building:

```
docker run -p8080:8080 hypersuck:latest
```

**HYPEREXEC**: full path to Hyper executables packaged with Hyper API (`lib/hyper` within that package as of 
0.0.10622.) Defaults to `/hyperapi/lib/hyper` under assumption you'll use the container that's extracted
the package there.

**PORT**: the port to bind to. Defaults to `8080`.

## Use

Get .hyper filenames within a .twbx (redirected from a .twb):

```
http://localhost:8080/filenames?url=https://public.tableau.com/workbooks/DPHIdahoCOVID-19Dashboard_V2.twb
```

Then get data, given a filename:

```
http://localhost:8080/data?url=https://public.tableau.com/workbooks/DPHIdahoCOVID-19Dashboard_V2.twb&filename=County%20(COVID%20State%20Dashboard.V1).hyper
```

## Deploy

Although I'll do CI eventually, I can manually version and push a `docker build` result to GCR as follows:

```
docker tag hypersuck:latest us.gcr.io/hypersuck/hypersuck:v1
docker push us.gcr.io/hypersuck/hypersuck:v1
```

