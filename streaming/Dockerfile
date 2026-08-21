FROM tiangolo/nginx-rtmp

COPY nginx.conf /etc/nginx/nginx.conf

# 1935: RTMP 송출, 8081: HLS 재생 (nginx.conf 의 listen 과 일치해야 한다)
EXPOSE 1935 8081
