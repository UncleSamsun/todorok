FROM alpine:3.22
RUN apk add --no-cache bash curl jq
WORKDIR /connect
COPY connector-template.json register-connector.sh ./
RUN chmod +x register-connector.sh
ENTRYPOINT ["/connect/register-connector.sh"]
