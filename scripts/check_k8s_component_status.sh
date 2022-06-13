namespace=$1
manager_name=manager
podname=`bash -i -c "sudo kubectl get pods -n ${namespace} "|grep ${manager_name} |awk '{print $1}'|head -1`
chmod +x /opt/pipeline/scripts/wait-for-it.sh
sudo kubectl cp -n ${namespace} /opt/pipeline/scripts/wait-for-it.sh ${podname}:/home/bingouser/wait-for-it.sh

sudo kubectl exec -ti ${podname} -n ${namespace} -- su bingouser << EOF
cd ~
./wait-for-it.sh $1:$2 --timeout=$3 --strict
EOF