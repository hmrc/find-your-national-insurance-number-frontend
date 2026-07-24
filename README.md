# Find your National Insurance number frontend
=================================================

Allows users to find their National Insurance number online where possible, and via a letter where not.

Requirements
------------

This service is written in [Scala 3.x](http://www.scala-lang.org/) and [Play 3.x](http://playframework.com/), so needs at least a [JRE 21](http://www.oracle.com/technetwork/java/javase/downloads/index.html) to run.

How to run locally
------------------

### PDV failure journey

Start this service and dependent services through service manager with the profile `FMN_ALL`. The service name in sm2 is `FIND_YOUR_NATIONAL_INSURANCE_NUMBER_FRONTEND` at port `14033`.

1. Go to the auth login stub `http://localhost:9949/auth-login-stub/gg-sign-in` and enter the following details: <br/>
   CredId: `pdv-success-nino` <br/>
   RedirectUrl: `http://localhost:14033/find-your-national-insurance-number/checkDetails?origin={PDV or IV}`<br/>
2. Click submit and follow the page directions for getting the number posted.

### CL50 journey with PDV integration

Start this service and dependent services through service manager with the command:

`sm2 --start FMN_PDV_TRACE --appendArgs '{"FIND_YOUR_NATIONAL_INSURANCE_NUMBER_FRONTEND": ["-Dmicroservice.services.personal-details-validation.port=9967"]}' --delay-seconds 5`

This configures the PDV back end in place of the stub used in the PDV failure journey. The service name in sm2 is `FIND_YOUR_NATIONAL_INSURANCE_NUMBER_FRONTEND` at port `14033`.

1. Go to the auth login stub `http://localhost:9949/auth-login-stub/gg-sign-in` and enter the following details: <br/>
   RedirectUrl: `http://localhost:14033/find-your-national-insurance-number/identity-confirmation`<br/>
2. Click submit and follow the page directions for getting the number posted.

Locally `sca-nino-stubs` is stubbing the data for all the required scenarios of this service. Please refer to its documentation for more details.

How to test the project
=======================

Unit Tests
----------
- **Unit test the entire test suite:** `sbt test`

- **Unit test a single spec file:** `sbt "test:testOnly *fileName"` (for example: `sbt "test:testOnly *CheckDetailsControllerSpec"`)

Integration tests
-----------------
- **`sbt it/test`**

Acceptance tests
----------------
To verify the acceptance tests locally, follow the steps:
- start the sm2 container for the FMN profile: `sm2 --start FMN_ALL`
- stop `FIND_YOUR_NATIONAL_INSURANCE_NUMBER_FRONTEND` process running in sm2: `sm2 --stop FIND_YOUR_NATIONAL_INSURANCE_NUMBER_FRONTEND`
- launch the service in terminal and execute the following command in the project directory: <br> `sbt "run -Dapplication.router=testOnlyDoNotUseInAppConf.Routes"`
- open [find-your-ni-number-acceptance-tests](https://github.com/hmrc/find-your-ni-number-acceptance-tests) repository in the terminal and run the local acceptance tests there

License
-------

This code is open source software licensed under the Apache 2.0 License.
