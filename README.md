
# find-your-national-insurance-number-frontend

## About
This is the frontend repository for the Find your National Insurance number service. It guides users to find their National Insurance numbers online where possible, and via a letter where not. 

## How to run locally (failure journey from pdv)
To run locally, start this service and dependant services through the service manager with the profile `FMN_ALL`. The service name in sm2 is `FIND_YOUR_NATIONAL_INSURANCE_NUMBER_FRONTEND` at port `14033`

1. Go to the auth login stub `http://localhost:9949/auth-login-stub/gg-sign-in` and enter the following details: <br/>
    CredId: `pdv-success-nino` <br/>
    RedirectUrl: `http://localhost:14033/find-your-national-insurance-number/checkDetails?origin={PDV or IV}`<br/>
2. Click submit and follow the page directions for getting the number posted

## How to run locally (CL50 journey with pdv integration)
To run locally, start this service and dependant services through the service manager with the command:
`sm2 --start FMN_PDV_TRACE --appendArgs '{"FIND_YOUR_NATIONAL_INSURANCE_NUMBER_FRONTEND": ["-Dmicroservice.services.personal-details-validation.port=9967"]}' --delay-seconds 5`. 

(This will configure PDV back end in place of the stub used in the pdv failure journey).

The service name in sm2 is `FIND_YOUR_NATIONAL_INSURANCE_NUMBER_FRONTEND` at port `14033`

1. Go to the auth login stub `http://localhost:9949/auth-login-stub/gg-sign-in` and enter the following details: <br/>
   RedirectUrl: `http://localhost:14033/find-your-national-insurance-number/identity-confirmation`<br/>
2. Click submit and follow the page directions for getting the number posted

Locally `sca-nino-stubs` is stubbing the data for all the required scenarios of this service. Please refer to its documentation for more details

### License
This code is open source software licensed under the Apache 2.0 License