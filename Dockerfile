# Try to produce ssh keys on Dockerfile. If cant use newly created ssh keys. That key should be uniqu for that project.

FROM jenkins/jenkins

ENV VIRTUAL_ENV=/opt/venv
ENV PATH="$VIRTUAL_ENV/bin:$PATH"

USER root
RUN apt-get update && apt-get install -y vim python3 python3-pip python3-dev sshpass python3-venv wget nano

# Installing the package
RUN wget https://obs-community-intl.obs.ap-southeast-1.myhuaweicloud.com/obsutil/current/obsutil_linux_amd64.tar.gz
RUN mkdir -p /usr/local/obsutil \
  && tar -C /usr/local/obsutil -xvf obsutil_linux_amd64.tar.gz --strip-components=1 \
  && chmod -R 755 /usr/local/obsutil \
  && mkdir /obsutil \
  && chmod -R 777 /obsutil

RUN /bin/bash -c "./usr/local/obsutil/setup.sh /usr/local/obsutil/obsutil"

# Adding the package path to local
ENV PATH="/usr/local/obsutil:$PATH"
RUN echo "export PATH="/usr/local/obsutil:$PATH"" > /etc/environment
# Ansible configuration
COPY ansible /opt/ansible
COPY robots /opt/robots
COPY scripts /scripts
RUN chmod -R 777 /scripts
RUN chown -R jenkins:jenkins /scripts 
# Robot Framework configuration

RUN mkdir /opt/venv

RUN chown -R jenkins:jenkins /opt/venv  

RUN chown -R jenkins:jenkins /opt/robots
RUN chown -R jenkins:jenkins /opt/ansible
COPY --chown=jenkins:jenkins .ssh /var/jenkins_home/.ssh

RUN chmod -R 600 /var/jenkins_home/.ssh

# Jenkins configuration
USER jenkins

RUN python3 -m venv $VIRTUAL_ENV
RUN python3 -m pip install -r /opt/robots/requirements.txt
RUN python3 -m pip install -r /opt/ansible/requirements.txt

RUN  mkdir -p /var/jenkins_home/backups && chown -R jenkins:jenkins /var/jenkins_home/backups

COPY dsl /opt/deploy/dsl

ENV JAVA_OPTS -Djenkins.install.runSetupWizard=false -Dhudson.model.DirectoryBrowserSupport.CSP=
ENV JENKINS_OPTS --argumentsRealm.roles.user=admin --argumentsRealm.passwd.admin=admin --argumentsRealm.roles.admin=admin

COPY --chown=jenkins:jenkins jenkins/plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN /usr/local/bin/install-plugins.sh < /usr/share/jenkins/ref/plugins.txt

COPY jenkins/casc.yaml /var/jenkins_home/casc.yaml
ENV CASC_JENKINS_CONFIG /var/jenkins_home/casc.yaml

ENV JENKINS_ADMIN_ID admin
ENV JENKINS_ADMIN_PASSWORD=admin


