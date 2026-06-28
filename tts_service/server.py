import io
import re
from urllib.parse import unquote
from flask import Flask, request, Response
from gtts import gTTS
from pydub import AudioSegment

app = Flask(__name__)


def sanitize_for_speech(text):
    text = re.sub(r'https?://\S+', '', text)
    text = re.sub(r'[*_#~`]', '', text)
    text = re.sub(r'\[([^\]]+)\]\([^)]+\)', r'\1', text)
    text = text.replace('|', ', ')
    text = re.sub(r'^[-*+]\s+', '', text, flags=re.MULTILINE)
    text = re.sub(r'\n{2,}', '. ', text)
    text = text.replace('\n', ' ')
    text = re.sub(r'\s+', ' ', text).strip()
    return text


@app.route('/health')
def health():
    return Response('ok', status=200, mimetype='text/plain')


@app.route('/api/tts')
def tts():
    text = request.args.get('text', '')
    if not text:
        return Response('text parameter is required', status=400)
    try:
        clean = sanitize_for_speech(unquote(text))
        mp3_fp = io.BytesIO()
        tts_instance = gTTS(text=clean, lang='pt-BR')
        tts_instance.write_to_fp(mp3_fp)
        mp3_fp.seek(0)
        audio = AudioSegment.from_mp3(mp3_fp)
        wav_fp = io.BytesIO()
        audio.export(wav_fp, format='wav')
        wav_fp.seek(0)
        return Response(wav_fp.read(), mimetype='audio/wav')
    except Exception as e:
        return Response(str(e), status=500)


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5002)
