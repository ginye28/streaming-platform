FROM tiangolo/nginx-rtmp

COPY nginx.conf /etc/nginx/nginx.conf

# 1935: RTMP 송출, 8081: HLS 재생 (nginx.conf 의 listen 과 일치해야 한다)
EXPOSE 1935 8081

# 콜백 주소의 비밀 값. API 의 RTMP_CALLBACK_TOKEN 과 같아야 한다. 비우면 API 가 검사하지 않는다.
ENV RTMP_CALLBACK_TOKEN=

# nginx.conf 는 환경변수를 못 읽어서, 뜰 때 자리표시자를 값으로 바꾼다. (구분자 | 라서 값에 / 가 있어도 된다)
CMD ["sh", "-c", "sed -i \"s|__RTMP_CALLBACK_TOKEN__|${RTMP_CALLBACK_TOKEN}|g\" /etc/nginx/nginx.conf && exec nginx -g 'daemon off;'"]
