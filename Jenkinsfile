@Library('wir-shared-lib') _

pipeline {
    agent any

    parameters {
        string(
            name: 'IMAGE_TAG',
            defaultValue: '',
            description: 'Docker image tag (git SHA, GitHub Actions에서 전달)'
        )
        string(
            name: 'BATCH_IMAGE_TAG',
            defaultValue: '',
            description: 'batch Docker image tag'
        )
    }

    environment {
        DEPLOY_ENV = "${JOB_NAME.contains('prod') ? 'prod' : 'dev'}"
    }

    stages {
        stage('Deploy') {
            steps {
                deploySpring(env: DEPLOY_ENV, imageTag: IMAGE_TAG, batchImageTag: params.BATCH_IMAGE_TAG)
            }
        }
    }

    post {
        success {
            notifySlack(
                status:  'success',
                service: 'backend',
                env:     DEPLOY_ENV,
                tag:     IMAGE_TAG
            )
        }
        failure {
            notifySlack(
                status:  'failure',
                service: 'backend',
                env:     DEPLOY_ENV,
                tag:     IMAGE_TAG
            )
        }
    }
}
