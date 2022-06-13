folder('az1-migrate-to-az2') {
    displayName('AZ1 MIGRATE TO AZ2')
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
    stage('COPY SCRIPTS TO AZ1 and AZ2'){
        sh """scp  -rpq /scripts/* ${SERVER_USER}@${K8S_AZ1_NODE_IP}:${SCRIPTS_PATH}/"""
        sh """scp  -rpq /scripts/* ${SERVER_USER}@${K8S_AZ2_NODE_IP}:${SCRIPTS_PATH}/"""
    }
    stage('RUN BACKUP SCRIPTS AZ1'){
        sh """ssh -t ${SERVER_USER}@${K8S_AZ1_NODE_IP} -o StrictHostKeyChecking=no \\"sudo sh ${SCRIPTS_PATH}/etcd-backup.sh ${K8S_AZ1_NODE_NAMESPACE_NAME} ${BACKUP_PATH}\\""""
    }
    stage('BACKUP FILES'){
        sh """ssh -t ${SERVER_USER}@${K8S_AZ1_NODE_IP} -o StrictHostKeyChecking=no \\"sudo chown ${SERVER_USER}:${SERVER_USER} ${BACKUP_PATH}/backup.tar.gz & sudo chmod 777 ${BACKUP_PATH}/backup.tar.gz\\""""
        sh """scp -rpq ${SERVER_USER}@${K8S_AZ1_NODE_IP}:${BACKUP_PATH}/backup.tar.gz /var/jenkins_home/backups/etcd/etcd_backup_${datePart}.tar.gz"""  
    }
    stage('COPY BACKUP FILE AZ2'){
        sh """scp -rpq /var/jenkins_home/backups/etcd/etcd_backup_${datePart}.tar.gz ${SERVER_USER}@${K8S_AZ2_NODE_IP}:${BACKUP_PATH}/restore.tar.gz"""
    }
    stage('RUN RESTORE SCRIPTS AZ2'){
        sh """ssh -t ${SERVER_USER}@${K8S_AZ2_NODE_IP} -o StrictHostKeyChecking=no \\"sudo sh ${SCRIPTS_PATH}/etcd-restore.sh ${K8S_AZ2_NODE_NAMESPACE_NAME} ${BACKUP_PATH}\\""""
    }
    
    stage('WAIT FOR DEPLOYMENT READINESS ON AZ2'){
        sh """ssh -t ${SERVER_USER}@${K8S_AZ2_NODE_IP} -o StrictHostKeyChecking=no \\"sudo kubectl wait -n 2 --for=condition=available --timeout=20m --all deployments\\""""
    }2
    stage('CLEAN OLDER BACKUP FILES'){
        sh """cd /var/jenkins_home/backups/etcd && ls -t | tail -n +11 | xargs rm -rf --"""  
    }

}

'''.stripIndent()

pipelineJob('az1-migrate-to-az2/BACKUP-TASKS/etcd_backup_restore') {
    parameters {
        stringParam( 'Migration', '10.36.51.44' )
        stringParam( 'K8S_AZ1_NODE_NAMESPACE_NAME', '1' )
        stringParam( 'K8S_AZ2_NODE_IP', '10.36.179.44' )
        stringParam( 'K8S_AZ2_NODE_NAMESPACE_NAME', '2' )
        stringParam( 'SERVER_USER', 'pipeline' )
        stringParam( 'SCRIPTS_PATH', '/opt/pipeline/scripts' )
        stringParam( 'BACKUP_PATH', '/opt/pipeline' )
    }

    properties {
        pipelineTriggers {
            triggers {
                cron {
                    spec('0 * * * *')
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