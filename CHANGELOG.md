# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.4.2-rc1] - 2022-10-17

### Added
N/A

### Changed
- RS-623 / RS-624: [BUG] [OPS] Sentinel-1 AIO preparation job stuck in GENERATING state with mandatory files not found. Backport from 1.5.0.
  WARNING: When using this version ensure that the configuration 'app.preparation-worker.process.hostname' for the RS Add-on is not set to ${HOSTNAME}, but a static unique name.
- RS-635: Sentinel-2 footprints are not added to PRIP index correctly

### Removed
N/A

## [1.4.1-rc1] - 2022-09-22

### Added
N/A

### Changed
- RS-XX: Backport documentation from develop to branch 1.4
- RS-560: Inventory_Metadata.xml must not be mandatory for Metadata Extraction

### Removed
N/A

## [1.4.0-rc1] - 2022-08-03

### Added

- RS-231: [SCDF] Implementation of a solution for "dead letter queue mechanism"
- RS-280: [BUG] PRIP does not return expected products when geographical request crosses antimeridian
- RS-344: [SCDF] Implementation long running processing feature
- RS-408: [PI] Update header traces with rs_chain_name_string & rs_chain_version_string
- RS-444: [PI] Add output to MetadataExtraction END OK trace
- RS-445: [PI] Update JobProcessing END traces with ICD Traces tailored



### Changed

- RS-429 / RS-436: Implement workaround to ignore S3 intersection error
- RS-449: Move from “filename_strings” to “filename_string” when single element
- RS-465: [BUG] bandIndexId 8A does not return result in the metadata catalog searchcontroller

### Removed
- RS-435: Move UWC helm chart CI to UWC repository

## [1.3.0-rc1] - 2022-07-06

### Added
- RS-248: Update JobProcessing traces
- RS-313: Develop "DLQ" as RS core component
- RS-314: Develop DLQ Manager application
- RS-315: Move ParkingLot from RequestRepository to RS Core DLQ restart
- RS-386: Migration of Execution Worker to SCDF Application
- RS-407: Update MetadataExtraction traces
- RS-422: Request the AUX GIP_VIEDIR on rs-metadata-catalog-searchcontroller-svc by band

### Changed
- RS-402: Missing documentation for mongodb
- RS-425: S2 HKTM and AUX_SAD metadata extraction fails with bad regex
- RS-409: Remove affinity from pro-common Helm chart

### Removed
N/A


## [1.2.0-rc1] - 2022-06-08

### Added
- RS-387: Migration of Preparation Worker to SCDF Application
- RS-207: Develop "DATALIFECYCLE" as a RS-core component

### Changed
- RS-388: Request Chunks and DSIB through Search Metadata Controller
- RS-373: RS-Addon: ingestion and metadata add-ons have multiple wrong deployment variables
- RS-391: Update RS Core to be in line with ICD RS Core release V5.0
- RS-372: Documents, code and ICD dont form a coherent whole for RS Core Catalog
- RS-392: Update RS Core to be in line with ICD Message Format ICD V3.0

## Removed
- N/A

## [1.1.2-rc1] - 2022-05-30

### Added
- N/A

### Changed
- RS-397: Fix: compression-worker: one of two s2 auxiliary directory format is not taken into account
- RS-398: Fix: Nullpointer exception when query S2_AUX via SearchMetadata Controller

### Removed
- N/A

## [1.1.1-rc1] - 2022-05-20

### Added
- N/A

### Changed
- RS-396: Fix: Metadata extraction for EDRS session fails with NumberFormatException

### Removed
- N/A

## [1.1.0-rc1] - 2022-05-13

### Added
- RS-206: RS Core component "Distribution" was added
- RS-214: RS Core component "Compression" was added

### Changed
- Fixed RS Core component ingestion pushing to wrong named destination. Using "catalog jobs" instead to be in line with examples.
- RS-240: Improving configuration and documentation on how to set JVM XMX argument to RS Core components 
- RS-351: PRIP and metadata-catalog-worker errors for OPER_MPL_TLEPRE files
- RS-364: User web client statefulset template has no keycloak parameter
- RS-384: Ingestion jobs do not trigger the metadata catalog

### Removed
- N/A

## [1.0.1-rc1] - 2022-04-28

This version is the first delivery for V1.1 and contains migrated components for SCDF workflows for Ingestion and Metadata Extraction.

### Added
- RS-212: Develop "Catalog" as RS-Core component
- RS-321: Split MDC Worker into extraction workflow and standalone query interface
- RS-330: Create factory ingestion trigger app
- RS-331: Create factory ingestion filter app
- RS-332: Create factory ingestion worker app

### Changed
- N/A

### Removed
- N/A

## [0.3.0-rc14] - 2022-03-16
### Added
- N/A

### Changed
- N/A

### Removed
- N/A

## [0.3.0-rc13] - 2022-03-16
### Added
- N/A

### Changed
- RS-XX: Adding tar command to ACQ container

### Removed
- N/A

## [0.3.0-rc12] - 2022-03-16
### Added
- N/A

### Changed
- RS-316: Creating .LIST file correctly also for high priority ISIPs
- RS-XX: Avoid deleting the working directory itself

### Removed
- N/A

## [0.3.0-rc11] - 2022-03-01
### Added
- N/A

### Changed
- RS-285: Changing the way how the working directory will handle AccessDeniedExceptions

### Removed
- N/A

## [0.3.0-rc10] - 2022-02-24
### Added
- N/A

### Changed
- RS-282: Fixing wrong image usage for S3 L0 IPF enforcing always to use simulator
- RS-283: Fixing typo in AUXIP configuration causing restarts
- RS-286: Fixing issue causing S3 L0 IPF to parse wrong .LIST file

### Removed
- N/A

## [0.3.0-rc9] - 2022-02-22
### Added
- N/A

### Changed
- RS-275: Using symbolic links again to workaround K8s partition layout
- RS-275: Adding host specific configuration mock

### Removed
- N/A

## [0.3.0-rc8] - 2022-02-21
### Added
- N/A

### Changed
- RS-275: Rebuild: Modified the behaviour of the wrapper script to handle inputs

### Removed
- N/A

## [0.3.0-rc7] - 2022-02-18
### Added
- N/A

### Changed
- RS-211: Removed configuration on not used ASP topics
- RS-275: Modified the behaviour of the wrapper script to handle inputs

### Removed
- N/A

## [0.3.0-rc6] - 2022-02-14
### Added
- N/A

### Changed
- N/A (rebuild)

### Removed
- N/A

## [0.3.0-rc5] - 2022-02-02
### Added
- N/A

### Changed
- Adding boost-date-time library for S3 ACQ container

### Removed
- N/A

## [0.3.0-rc3] - 2022-02-02
### Added
- N/A

### Changed
- RS-250: Ensure the right header is used for OUTH2 authorization

### Removed
- N/A

## [0.3.0-rc2] - 2022-01-31
### Added
- N/A

### Changed
- RS-250: Old servers expecting a bearer token named "OAUTH2-ACCESS-TOKEN". New server is following the specification. Behaviour was changed according to specification.

### Removed
- N/A

## [0.3.0-rc1] - 2022-01-18
### Added
- RS-150: S1 L0 AIOP/ASP containers with real IPFs are build
- RS-151: S3 ACQ containers with real IPFs are build
- RS-156: S3 L0 containers with real IPFs are build
- RS-205: Data Lifecycle Manager had been migrated from S1PRO
- RS-108: Amount of detected security incidents was decreased
- RS-200: S3 footprints are now extracted correctly and propagated to the PRIP
- RS-144: Adding interface query list support in DDIP
- RS-183, RS-170: Allowing to specify a configuration branch different to the software branch for rs_init
- RS-152: Mock for XBIP and EDIP had been pushed to repository under rs-testing
- RS-159: Documentation updated on where to use rs_init
- RS-182: Modifying native api service to allow datetime queries using STAC API
- RS-XX:  Added ingestion filter for EDIP

### Changed
- RS-XX: Removed s1pro-env-scale.sh as it is not used anymore. Use replicaCount.yaml from template folder to setup the amount of replicas instead

### Removed
- N/A


## [0.2.0-rc1] - 2021-12-15
### Added
- RS-101: Filtering solution for S1 and S3 chunks at ingestion
- RS-105: Integration of S3 Acquisition Workflow into S1PRO software
- RS-106: Integration of S3 L0P workflow
- RS-127: Management of PUG processing of S3 L0 products
- RS-133: Integration of EDIP
- RS-134: Integration of AUXIP
- RS-135: Creation of DDIP Fascade
- RS-155: Adding additional XBIP instances to Reference System

### Changed
- RS-136: User Web Client is able to communicate with DDIP
- RS-137: Implementation of collection support within DDIP
- RS-139: Adding support for point and line queries in PRIP
- RS-159: Removing dependency on s1pro-env-init.sh
- RS-171: QCSS was removed from the base configuration to fix the reported issue

### Removed
- RS-168: Removing Kong/Ingress config from PRIP frontend

## [0.1.0-rc1] - 2021-11-17
### Added
- RS-21: RS API allowing to perform a standard product download
- RS-110: Adding a script "rs_deploy" allowing to deploy and remove specific RS addons
- RS-104: Adding basic deployment for RS API service
- RS-111: An init script to setup an RS environment and fetch required repositories from Github


### Changed
- RS-36: Modification of build pipes in rs-core and push artifacts into Artifactory now instead of GHCR
- RS-88: Migration of documentation from repository rs-documentatiojn into repository reference-system-documentation
- RS-109: Migration of Sentinel-3 CGS Transformation configuration into RS
- RS-113: Migrating existing S1PRO repository structure in the repository structure from the consortium

### Removed
- N/A
