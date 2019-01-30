pipeline {
    agent {
        label 'ubuntu'
    }
    
    tools {
        jdk 'JDK 1.8 (latest)'
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timestamps()
    }

    triggers {
        pollSCM('H/15 * * * *')
    }

    stages {
        stage('SCM Checkout') {
            steps {
                deleteDir()
                checkout scm
            }
        }
        
        stage('Check environment') {
            steps {
                sh 'env'
                sh 'pwd'
                sh 'ls'
                sh 'git status'
            }
        }

        stage('Run tests') {
            steps {
                // use install, as opposed to verify, to ensure invoker tests use latest code
                sh './mvnw clean install'
            }
        }
    }


    post {
        always {
            junit '**/target/surefire-reports/*.xml'
            deleteDir()
        }
    }
}eteDir()
        }
    }
}
