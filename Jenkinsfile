pipeline {
    agent any

    environment {
        REGISTRY = "enriquebravo115"
        IMAGE_NAME = "users-service"
        IMAGE_TAG = "latest"
        EKS_CLUSTER = "your-eks-cluster"
        AWS_REGION = "us-east-1"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test') {
            steps {
                sh './mvnw clean package -DskipTests=false'
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    sh "docker build -t $REGISTRY/$IMAGE_NAME:$IMAGE_TAG ."
                }
            }
        }

        stage('Push Docker Image') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub-creds', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                    sh """
                        echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin
                        docker push $REGISTRY/$IMAGE_NAME:$IMAGE_TAG
                    """
                }
            }
        }

        stage('Deploy to EKS') {
            steps {
                withCredentials([file(credentialsId: 'aws-kubeconfig', variable: 'KUBECONFIG_FILE')]) {
                    sh """
                        export KUBECONFIG=$KUBECONFIG_FILE
                        kubectl config use-context arn:aws:eks:$AWS_REGION:<your-aws-account-id>:cluster/$EKS_CLUSTER

                        # Reemplaza esto con un apply o rollout restart de tu Deployment
                        kubectl set image deployment/users-deployment users=$REGISTRY/$IMAGE_NAME:$IMAGE_TAG -n your-namespace
                    """
                }
            }
        }
    }

    post {
        success {
            echo "✅ Despliegue exitoso en EKS"
        }
        failure {
            echo "❌ Algo falló"
        }
    }
}
