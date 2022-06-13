# lydia-soft
## Building Project
Run ``` docker build -t lydia-soft .```

### Check Docker Image

Run `` docker images```

You need to see lydia-sofy name there


## Run The Project

``` docker run -d -p 8081:8080 -v ~/jenkins/backups:/var/jenkins_home/backups -v ~/jenkins/jobs:/var/jenkins_home/jobs lydia-soft ```

### Check the status

``` docker ps ```

You should see status as health

## Login to Jenkins

Go to ``` localhost:8081 ```.

You should see jenkins login page. Initial informations:

Username: admin
Password: admin