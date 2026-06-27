FROM python:3.11-slim

RUN pip install --no-cache-dir gtts flask pydub

RUN apt-get update && apt-get install -y --no-install-recommends ffmpeg && rm -rf /var/lib/apt/lists/*

COPY server.py /server.py

EXPOSE 5002

CMD ["python3", "/server.py"]
