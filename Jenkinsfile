pipeline {
    agent any

    environment {
        IMAGE_NAME = 'chatbot-app'
        CONTAINER_NAME = 'chatbot-app'
        HEALTH_CHECK_URL = 'http://13.125.47.3:8082/actuator/health'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh '''
                chmod +x ./gradlew
                ./gradlew build -x test
                '''
            }
        }

        stage('Prepare Files') {
            steps {
                withCredentials([
                    file(credentialsId: 'chatbot-application.yml', variable: 'APP_YML')
                ]) {
                    sh '''
                    cp "$APP_YML" ./application.yml
                    chmod 644 ./application.yml
                    '''
                }
            }
        }

        stage('Deploy to Chatbot EC2') {
            steps {
                sshPublisher(
                    publishers: [
                        sshPublisherDesc(
                            configName: 'chatbot-ec2',
                            transfers: [
                                sshTransfer(
                                    sourceFiles: 'Dockerfile,build/libs/*.jar,application.yml',
                                    removePrefix: 'build/libs',
                                    remoteDirectory: 'chatbot',
                                    execCommand: '''
                                        cd /home/ubuntu/chatbot

                                        chmod +x gradlew

                                        docker stop chatbot-app || true
                                        docker rm chatbot-app || true

                                        docker build -t chatbot-app:latest .

                                        docker run -d \
                                          --name chatbot-app \
                                          --restart unless-stopped \
                                          --log-driver json-file \
                                          --log-opt max-size=50m \
                                          --log-opt max-file=5 \
                                          -p 8082:8082 \
                                          -v /home/ubuntu/chatbot/application.yml:/app/application.yml:ro \
                                          chatbot-app:latest \
                                          --spring.config.location=file:/app/application.yml

                                        docker image prune -f
                                    '''
                                )
                            ],
                            verbose: true
                        )
                    ]
                )
            }
        }

        stage('Health Check') {
            steps {
                script {
                    def maxRetry = 10
                    def success = false

                    for (int i = 0; i < maxRetry; i++) {
                        def status = sh(
                            script: "curl -s ${HEALTH_CHECK_URL} | grep UP || true",
                            returnStdout: true
                        ).trim()

                        if (status.contains("UP")) {
                            success = true
                            echo "Health Check 성공"
                            break
                        }

                        echo "Health Check 재시도 중... (${i + 1}/${maxRetry})"
                        sleep 5
                    }

                    if (!success) {
                        error("Health Check 실패")
                    }
                }
            }
        }
    }

    post {
        success {
            script {
                def author = sh(
                    script: "git log -1 --pretty=format:'%an'",
                    returnStdout: true
                ).trim()

                def message = sh(
                    script: "git log -1 --pretty=format:'%s'",
                    returnStdout: true
                ).trim()

                def duration = currentBuild.durationString
                    .replace(' and counting', '')

                withCredentials([
                    string(credentialsId: 'discord-webhook', variable: 'DISCORD_WEBHOOK')
                ]) {
                    writeFile file: 'discord-success.json', text: """
                    {
                      "content": "✅ 챗봇 서버 배포 성공\\n작성자: ${author}\\n커밋: ${message}\\n실행 시간: ${duration}\\n빌드 번호: #${BUILD_NUMBER}\\nURL: ${BUILD_URL}"
                    }
                    """

                    sh '''
                    curl -H "Content-Type: application/json" \
                         -X POST \
                         -d @discord-success.json \
                         "$DISCORD_WEBHOOK"
                    '''
                }
            }
        }

        failure {
            script {
                def author = sh(
                    script: "git log -1 --pretty=format:'%an' || echo unknown",
                    returnStdout: true
                ).trim()

                def message = sh(
                    script: "git log -1 --pretty=format:'%s' || echo unknown",
                    returnStdout: true
                ).trim()

                def duration = currentBuild.durationString
                    .replace(' and counting', '')

                withCredentials([
                    string(credentialsId: 'discord-webhook', variable: 'DISCORD_WEBHOOK')
                ]) {
                    writeFile file: 'discord-failure.json', text: """
                    {
                      "content": "❌ 챗봇 서버 배포 실패\\n작성자: ${author}\\n커밋: ${message}\\n실행 시간: ${duration}\\n빌드 번호: #${BUILD_NUMBER}\\nURL: ${BUILD_URL}"
                    }
                    """

                    sh '''
                    curl -H "Content-Type: application/json" \
                         -X POST \
                         -d @discord-failure.json \
                         "$DISCORD_WEBHOOK"
                    '''
                }
            }
        }
    }
}
