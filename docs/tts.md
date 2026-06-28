# Serviço de Síntese de Voz (TTS)

## Implementação

O serviço TTS é um servidor Flask Python que usa a biblioteca `gTTS`
(Google Text-to-Speech) para sintetizar voz em português brasileiro.

## Fluxo

1. A API Spring Boot envia GET para `http://tts:5002/api/tts?text=...`
2. O servidor Flask recebe o texto e executa `sanitize_for_speech`
3. `gTTS` chama a API do Google Translate e retorna MP3
4. `pydub` + `ffmpeg` converte MP3 para WAV
5. O WAV é retornado como `audio/wav`

## sanitize_for_speech

Remove artefatos de markdown e formatos técnicos que seriam lidos
literalmente pelo TTS:
- URLs (http://...)
- Caracteres de formatação markdown (* _ # ~ `)
- Links markdown [texto](url) -> mantém apenas o texto
- Separadores de tabela (|)
- Bullets de lista (- * +)
- Quebras de linha excessivas

## Endpoints

- `GET /health` - Retorna 200 ok (usado pelo Docker healthcheck)
- `GET /api/tts?text={texto}` - Retorna audio/wav com o texto sintetizado

## Trade-offs

| Aspecto | gTTS |
|---|---|
| Qualidade | Alta (Google TTS) |
| Custo | Gratuito (limites de uso) |
| Latência | Depende de rede externa |
| Offline | Não funciona |
| Setup | Instantâneo (sem download de modelo) |

## Por que não Coqui TTS?

Coqui TTS foi considerado inicialmente por ser auto-hospedado (sem
dependência de rede externa). No entanto, o modelo CPU para português
(`tts_models/pt/cv/vits`) exige 2-5 minutos de download no primeiro
uso e tem qualidade inferior. O gTTS oferece melhor qualidade com
setup mais simples para um projeto de estudo.
