folder('az1-migrate-to-az2') {
    displayName('BACKUP TASKS')
}

folder('az1-migrate-to-az2/BACKUP-TASKS') {
    displayName('BACKUP TASKS')
}

String pipelineScript = '''
Date date = new Date()
String datePart = date.format("yyyyMMddHHmm")

node(){
    // Before everything copy ssh with ssh-copy-id to worker node. 
    stage('COPY SSH ID'){
            withCredentials([string(credentialsId: 'test', variable: 'test1')]) {
            sh """sshpass -p '${test1}' ssh-copy-id -i /var/jenkins_home/.ssh/id_rsa.pub -o StrictHostKeyChecking=no ${SERVER_USER}@${K8S_AZ1_NODE_IP}"""
            sh """sshpass -p '${test1}' ssh-copy-id -i /var/jenkins_home/.ssh/id_rsa.pub -o StrictHostKeyChecking=no ${SERVER_USER}@${K8S_AZ2_NODE_IP}"""
            }
    }
    stage('BACKUP K8S CONFIGS FROM AZ1'){
        sh """ssh -t ${SERVER_USER}@${K8S_AZ1_NODE_IP} -o StrictHostKeyChecking=no \\"sudo tar -zcvf k8s_backup.tar.gz /home/install && sudo chown ${SERVER_USER}:${SERVER_USER} k8s_backup.tar.gz\\""""
    }
    stage('BACKUP FILES TO SERVER'){
        sh """scp -rpq ${SERVER_USER}@${K8S_AZ1_NODE_IP}:k8s_backup.tar.gz /var/jenkins_home/backups/k8s/k8s_backup_${datePart}.tar.gz"""  
    }
    stage('UPLOAD BACKUP FILES TO AZ2'){
        sh """scp -rpq /var/jenkins_home/backups/k8s/k8s_backup_${datePart}.tar.gz ${SERVER_USER}@${K8S_AZ2_NODE_IP}:${BACKUP_PATH}/k8s_restore.tar.gz""" 
        sh """ssh -t ${SERVER_USER}@${K8S_AZ2_NODE_IP} -o StrictHostKeyChecking=no \\"sudo chown ${SERVER_USER}:${SERVER_USER} ${BACKUP_PATH}/k8s_restore.tar.gz\\""""
    }
    stage('RESTORE FILES'){
        sh """ssh -t ${SERVER_USER}@${K8S_AZ2_NODE_IP} -o StrictHostKeyChecking=no \\"sudo tar xC / -f ${BACKUP_PATH}/k8s_restore.tar.gz && sudo rm ${BACKUP_PATH}/*.tar.gz\\""""
    }

    stage('CLEAN OLDER BACKUP FILES'){
        sh """cd /var/jenkins_home/backups/k8s && ls -t | tail -n +11 | xargs rm -rf --"""  
    }
}
'''.stripIndent()


pipelineJob('az1-migrate-to-az2/BACKUP-TASKS/K8S_backup_restore') {
    parameters {
        stringParam( 'K8S_AZ1_NODE_IP', '10.363.31.112')
        stringParam( 'K8S_AZ2_NODE_IP', '10.563.31.112')
        stringParam( 'SERVER_USER', 'pipeline')
        stringParam( 'BACKUP_PATH', '/opt/pipeline')
    }

    properties {
        pipelineTriggers {
            triggers {
                cron {
                    spec('*/5 * * * *')
                }
            }
        }
    }

    definition {
        cps {
            script(pipelineScript)
            sandbox()
        }
    }
}