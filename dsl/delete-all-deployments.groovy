String pipelineScript = '''
def k8s_service_infoes = [
    "websocket",
    "nginxingresscontroller",
]
node(){
    // Before everything copy ssh with ssh-copy-id to worker node. 
    stage('COPY SSH ID'){
         withCredentials([string(credentialsId: 'SECRET', variable: 'SECRET')]) {
            sh """sshpass -p $SECRET ssh-copy-id -i /var/jenkins_home/.ssh/id_rsa.pub -o StrictHostKeyChecking=no root@${K8S_AZ_NODE_IP}"""
         }
         // Jenkins Credentials should be contain ssh password.
    }
    stage('DELETE STATELESS JOBS'){
        for (key in k8s_service_infoes) {
            status = sh (
                script: """ssh -tq root@${K8S_AZ_NODE_IP} -o StrictHostKeyChecking=no \\"kubectl delete deployment ${key} -n ${K8S_NAMESPACE} \\"""",
                returnStdout : true
            )
        }       
    } 
}
'''.stripIndent()

pipelineJob('Delete All Deployments(Only development Purposes DO NOT USE)') {
    disabled()
    parameters {
        stringParam( 'K8S_NAMESPACE', 'test' )
        stringParam( 'K8S_AZ_NODE_IP', '10.422.132.43' )
    }

    definition {
        cps {
          script(pipelineScript)
          sandbox()
        }
    }
}


