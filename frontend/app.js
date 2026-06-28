const STORAGE_KEY = 'budget-voice-api-base-url';

function getBaseUrl() {
  return localStorage.getItem(STORAGE_KEY) || 'http://localhost:8080';
}

function setBaseUrl(url) {
  localStorage.setItem(STORAGE_KEY, url);
}

async function apiFetch(path, options = {}) {
  const url = getBaseUrl().replace(/\/+$/, '') + path;
  const method = (options.method || 'GET').toUpperCase();
  const hasBody = options.body != null;
  const headers = { ...options.headers };
  if (hasBody && !(options.body instanceof FormData)) {
    headers['Content-Type'] = 'application/json';
  }
  const res = await fetch(url, { ...options, headers });
  return res;
}

function formatBRL(amount) {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(amount);
}

function formatDate(iso) {
  if (!iso) return '-';
  const d = new Date(iso);
  return d.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' });
}

function formatDateTime(iso) {
  if (!iso) return '-';
  const d = new Date(iso);
  return d.toLocaleString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' });
}

function escapeHtml(text) {
  const d = document.createElement('div');
  d.textContent = text;
  return d.innerHTML;
}

function createSpinner() {
  const el = document.createElement('div');
  el.className = 'spinner';
  return el;
}

function createBadge(text, type) {
  const el = document.createElement('span');
  el.className = `badge badge-${type}`;
  el.textContent = text;
  return el;
}

function createToast(message, type = 'success', duration = 4000) {
  const container = document.getElementById('toast-container');
  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  const iconMap = { success: '✓', error: '✗', warning: '⚠', info: 'ℹ' };
  const iconSpan = document.createElement('span');
  iconSpan.textContent = iconMap[type] || 'ℹ';
  const msgSpan = document.createElement('span');
  msgSpan.textContent = message;
  const closeBtn = document.createElement('button');
  closeBtn.className = 'toast-close';
  closeBtn.innerHTML = '&times;';
  closeBtn.onclick = () => toast.remove();
  toast.append(iconSpan, msgSpan, closeBtn);
  container.appendChild(toast);
  setTimeout(() => { if (toast.parentNode) toast.remove(); }, duration);
}

function statusLabel(status) {
  if (status < 300) return { text: `${status} OK`, type: 'success' };
  if (status < 500) return { text: `${status} Error`, type: 'warning' };
  return { text: `${status} Error`, type: 'error' };
}

function highlightJson(obj) {
  const json = JSON.stringify(obj, null, 2);
  return json.replace(/("(?:\\.|[^"\\])*")\s*:/g, '<span class="key">$1</span>:')
    .replace(/:(\s*)"((?:\\.|[^"\\])*)"/g, ':<span class="string">"$2"</span>')
    .replace(/:(\s*)(\d+(?:\.\d+)?)/g, ':<span class="number">$2</span>')
    .replace(/:(\s*)(true|false)/g, ':<span class="boolean">$2</span>')
    .replace(/:(\s*)(null)/g, ':<span class="null">$2</span>');
}

function createCodeBlock(data, expandable = true, initialExpanded = true) {
  const container = document.createElement('div');
  container.className = 'code-details';

  if (expandable) {
    const details = document.createElement('details');
    details.open = initialExpanded;
    const summary = document.createElement('summary');
    summary.textContent = 'Response JSON';
    details.appendChild(summary);

    const pre = document.createElement('pre');
    pre.className = 'code-block';
    pre.innerHTML = highlightJson(data);
    details.appendChild(pre);

    const copyBtn = document.createElement('button');
    copyBtn.className = 'btn-copy';
    copyBtn.textContent = 'Copy';
    copyBtn.onclick = () => {
      navigator.clipboard.writeText(JSON.stringify(data, null, 2));
      copyBtn.textContent = 'Copied!';
      setTimeout(() => { copyBtn.textContent = 'Copy'; }, 2000);
    };
    pre.appendChild(copyBtn);
    container.appendChild(details);
  } else {
    const pre = document.createElement('pre');
    pre.className = 'code-block';
    pre.innerHTML = highlightJson(data);
    const copyBtn = document.createElement('button');
    copyBtn.className = 'btn-copy';
    copyBtn.textContent = 'Copy';
    copyBtn.onclick = () => {
      navigator.clipboard.writeText(JSON.stringify(data, null, 2));
      copyBtn.textContent = 'Copied!';
      setTimeout(() => { copyBtn.textContent = 'Copy'; }, 2000);
    };
    pre.appendChild(copyBtn);
    container.appendChild(pre);
  }

  return container;
}

function setLoading(cardId, loading) {
  const card = document.getElementById(cardId);
  if (!card) return;
  if (loading) card.classList.add('loading');
  else card.classList.remove('loading');
}

function setResult(id, status, data) {
  const container = document.getElementById(id);
  if (!container) return;
  const badge = container.querySelector('.badge');
  const jsonContainer = container.querySelector('.json-result');
  if (badge) {
    const info = statusLabel(typeof status === 'number' ? status : (status ? 200 : 500));
    badge.className = `badge badge-${info.type}`;
    badge.textContent = info.text;
  }
  if (jsonContainer) {
    jsonContainer.innerHTML = '';
    jsonContainer.appendChild(createCodeBlock(data, true, false));
  }
}

async function checkHealth() {
  const statusBadge = document.getElementById('status-badge');
  const statusLabelEl = document.getElementById('status-label');
  statusBadge.className = 'header-badge offline';
  statusBadge.textContent = 'Checking...';
  try {
    const res = await apiFetch('/api/voice/health');
    const text = await res.text();
    if (res.ok && text.includes('running')) {
      statusBadge.className = 'header-badge online';
      statusBadge.textContent = 'Online';
      if (statusLabelEl) statusLabelEl.textContent = 'Budget Voice API is running';
      return true;
    } else {
      statusBadge.className = 'header-badge offline';
      statusBadge.textContent = 'Error';
      if (statusLabelEl) statusLabelEl.textContent = `HTTP ${res.status}: ${text}`;
      return false;
    }
  } catch (err) {
    statusBadge.className = 'header-badge offline';
    statusBadge.textContent = 'Offline';
    if (statusLabelEl) statusLabelEl.textContent = `Unreachable: ${err.message}`;
    return false;
  }
}

async function renderHealthSection() {
  setLoading('health-card', true);
  try {
    const res = await apiFetch('/api/voice/health');
    const text = await res.text();
    setResult('health-result', res.status, { status: res.status, body: text });
    if (res.ok && text.includes('running')) {
      document.getElementById('health-status-text').textContent = text;
      document.getElementById('health-status-text').className = 'result-text';
    } else {
      document.getElementById('health-status-text').textContent = `HTTP ${res.status}: ${text}`;
      document.getElementById('health-status-text').className = 'result-text';
    }
  } catch (err) {
    setResult('health-result', 0, { error: err.message });
    document.getElementById('health-status-text').textContent = `Failed: ${err.message}`;
    document.getElementById('health-status-text').className = 'result-text';
  }
  setLoading('health-card', false);
}

function initVoiceSection() {
  const uploadZone = document.getElementById('voice-upload-zone');
  const fileInput = document.getElementById('voice-file-input');
  const preview = document.getElementById('voice-preview');
  const sendBtn = document.getElementById('voice-send-btn');
  const micBtn = document.getElementById('voice-mic-btn');
  const result = document.getElementById('voice-result');
  let selectedFile = null;
  let mediaRecorder = null;
  let audioChunks = [];
  let isRecording = false;
  let recordingTimer = null;
  let recordingSeconds = 0;

  function getResponseType() {
    const selected = document.querySelector('input[name="voice-response-type"]:checked');
    return selected ? selected.value : 'text';
  }

  function formatFileSize(bytes) {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  }

  function updatePreview(file) {
    selectedFile = file;
    if (file) {
      uploadZone.classList.add('has-file');
      const ext = file.name.split('.').pop().toUpperCase();
      preview.innerHTML = `
        <div class="upload-preview">
          <span>🎵</span>
          <span class="upload-name">${escapeHtml(file.name)}</span>
          <span class="upload-size">${formatFileSize(file.size)}</span>
          <span class="audio-duration" id="audio-duration-label"></span>
          <span class="upload-remove" style="cursor:pointer;color:var(--color-accent-red);" title="Remover arquivo">✕</span>
        </div>`;
      preview.querySelector('.upload-remove').onclick = (e) => {
        e.stopPropagation();
        clearFile();
      };
      tryReadAudioDuration(file);
    } else {
      uploadZone.classList.remove('has-file');
      preview.innerHTML = `
        <div class="upload-placeholder">
          <svg viewBox="0 0 24 24" width="48" height="48" fill="none"
               stroke="var(--color-text-muted)" stroke-width="1.5">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
            <polyline points="17 8 12 3 7 8"/>
            <line x1="12" y1="3" x2="12" y2="15"/>
          </svg>
          <p>Clique ou arraste um arquivo de áudio (MP3, WAV, OGG, M4A)</p>
        </div>`;
    }
  }

  function tryReadAudioDuration(file) {
    const url = URL.createObjectURL(file);
    const audio = new Audio(url);
    audio.addEventListener('loadedmetadata', () => {
      const mins = Math.floor(audio.duration / 60);
      const secs = Math.floor(audio.duration % 60).toString().padStart(2, '0');
      const label = document.getElementById('audio-duration-label');
      if (label) label.textContent = `${mins}:${secs}`;
      URL.revokeObjectURL(url);
    });
    audio.addEventListener('error', () => URL.revokeObjectURL(url));
  }

  function clearFile() {
    fileInput.value = '';
    updatePreview(null);
  }

  function startRecordingTimer() {
    recordingSeconds = 0;
    micBtn.classList.add('btn-mic-recording');
    recordingTimer = setInterval(() => {
      recordingSeconds++;
      const mins = Math.floor(recordingSeconds / 60).toString().padStart(2, '0');
      const secs = (recordingSeconds % 60).toString().padStart(2, '0');
      micBtn.innerHTML = `
        <span class="recording-dot"></span>
        ${mins}:${secs}
      `;
    }, 1000);
  }

  function stopRecordingTimer() {
    if (recordingTimer) {
      clearInterval(recordingTimer);
      recordingTimer = null;
    }
    micBtn.classList.remove('btn-mic-recording');
    micBtn.textContent = '🎤 Gravar';
  }

  function showAutoRefreshHint() {
    const currentHash = window.location.hash;
    if (currentHash === '#transactions') {
      setTimeout(() => {
        renderBalanceSection();
        renderTransactionsSection(0);
      }, 800);
    }
  }

  uploadZone.addEventListener('click', () => fileInput.click());
  uploadZone.addEventListener('dragover', (e) => {
    e.preventDefault();
    uploadZone.classList.add('drag-over');
  });
  uploadZone.addEventListener('dragleave', () => uploadZone.classList.remove('drag-over'));
  uploadZone.addEventListener('drop', (e) => {
    e.preventDefault();
    uploadZone.classList.remove('drag-over');
    if (e.dataTransfer.files[0]) updatePreview(e.dataTransfer.files[0]);
  });
  fileInput.addEventListener('change', () => {
    if (fileInput.files[0]) updatePreview(fileInput.files[0]);
  });

  sendBtn.addEventListener('click', async () => {
    if (!selectedFile) {
      createToast('Selecione um arquivo de áudio primeiro', 'warning');
      return;
    }
    const responseType = getResponseType();
    sendBtn.disabled = true;
    sendBtn.textContent = 'Processando...';
    result.innerHTML = `
      <div style="display:flex;align-items:center;gap:12px;padding:20px;">
        <div class="spinner"></div>
        <span style="color:var(--color-text-secondary);font-size:14px;">
          Transcrevendo e processando comando...
        </span>
      </div>`;

    try {
      const formData = new FormData();
      formData.append('audio', selectedFile);
      const endpoint = responseType === 'audio'
        ? '/api/voice/command/audio'
        : '/api/voice/command';
      const res = await fetch(
        getBaseUrl().replace(/\/+$/, '') + endpoint,
        { method: 'POST', body: formData }
      );

      if (responseType === 'audio') {
        if (!res.ok) {
          const contentType = res.headers.get('content-type') || '';
          let errData;
          if (contentType.includes('json')) {
            errData = await res.json().catch(() => ({ error: res.statusText }));
          } else {
            errData = { error: await res.text().catch(() => res.statusText) };
          }
          result.innerHTML = '';
          const alert = document.createElement('div');
          alert.className = 'alert alert-error';
          alert.textContent = errData.error || `Erro HTTP ${res.status}`;
          result.appendChild(alert);
          if (res.status === 503) {
            const hint = document.createElement('p');
            hint.className = 'small text-muted';
            hint.style.marginTop = '8px';
            hint.textContent = 'O serviço de síntese de voz pode estar inicializando. Tente em alguns instantes.';
            result.appendChild(hint);
          }
        } else {
          const blob = await res.blob();
          const audioUrl = URL.createObjectURL(blob);
          result.innerHTML = '';
          const audioEl = document.createElement('audio');
          audioEl.className = 'audio-player';
          audioEl.controls = true;
          audioEl.src = audioUrl;
          audioEl.autoplay = true;
          result.appendChild(audioEl);
          const info = document.createElement('p');
          info.className = 'small text-muted';
          info.style.marginTop = '8px';
          info.textContent = `Resposta em áudio (${formatFileSize(blob.size)})`;
          result.appendChild(info);
          createToast('Resposta em áudio recebida', 'success');
          showAutoRefreshHint();
        }
      } else {
        const data = await res.json();
        result.innerHTML = '';
        const statusInfo = statusLabel(res.status);
        const badge = createBadge(statusInfo.text, statusInfo.type);

        const resultPanel = document.createElement('div');
        resultPanel.className = 'result-panel';
        resultPanel.appendChild(badge);

        if (data.transcribedText) {
          const tLabel = document.createElement('h3');
          tLabel.textContent = 'Transcrição';
          tLabel.style.marginTop = '12px';
          const tText = document.createElement('p');
          tText.className = 'result-text';
          tText.style.color = 'var(--color-text-secondary)';
          tText.textContent = data.transcribedText;
          resultPanel.appendChild(tLabel);
          resultPanel.appendChild(tText);
        }

        if (data.aiResponse) {
          const rLabel = document.createElement('h3');
          rLabel.textContent = 'Resposta da IA';
          rLabel.style.marginTop = '12px';
          const rText = document.createElement('p');
          rText.className = 'result-text';
          rText.textContent = data.aiResponse;
          resultPanel.appendChild(rLabel);
          resultPanel.appendChild(rText);
        }

        if (data.error) {
          const errDiv = document.createElement('div');
          errDiv.className = 'alert alert-error';
          errDiv.style.marginTop = '12px';
          errDiv.textContent = data.error;
          resultPanel.appendChild(errDiv);
        }

        result.appendChild(resultPanel);
        result.appendChild(createCodeBlock(data, true, false));

        const isSuccess = res.ok && !data.error;
        createToast(
          isSuccess ? 'Comando processado com sucesso' : 'Comando processado com ressalvas',
          isSuccess ? 'success' : 'warning'
        );
        if (isSuccess) showAutoRefreshHint();
      }
    } catch (err) {
      const isNetworkError = err.name === 'TypeError' && err.message.includes('Failed to fetch');
      const msg = isNetworkError
        ? `Servidor inacessível em ${getBaseUrl()}. Verifique se a API está rodando.`
        : `Erro inesperado: ${err.message}`;
      result.innerHTML = `<div class="alert alert-error">${escapeHtml(msg)}</div>`;
      createToast('Falha na requisição', 'error');
    }

    sendBtn.disabled = false;
    sendBtn.textContent = 'Enviar';
  });

  if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
    micBtn.addEventListener('click', async () => {
      if (isRecording) {
        mediaRecorder.stop();
        isRecording = false;
        stopRecordingTimer();
        return;
      }
      try {
        const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
        const mimeType = MediaRecorder.isTypeSupported('audio/webm')
          ? 'audio/webm'
          : 'audio/mp4';
        mediaRecorder = new MediaRecorder(stream, { mimeType });
        audioChunks = [];

        mediaRecorder.ondataavailable = (e) => {
          if (e.data.size > 0) audioChunks.push(e.data);
        };

        mediaRecorder.onstop = () => {
          const blob = new Blob(audioChunks, { type: mediaRecorder.mimeType });
          const ext = mediaRecorder.mimeType.includes('mp4') ? 'mp4' : 'webm';
          const file = new File([blob], `gravacao.${ext}`, { type: mediaRecorder.mimeType });
          updatePreview(file);
          stream.getTracks().forEach(t => t.stop());
          createToast('Gravação concluída. Clique em Enviar para processar.', 'info');
        };

        mediaRecorder.start();
        isRecording = true;
        startRecordingTimer();
        createToast('Gravando... Clique novamente para parar.', 'info');
      } catch {
        createToast('Acesso ao microfone negado ou não disponível', 'error');
      }
    });
  } else {
    micBtn.style.display = 'none';
  }
}

async function renderTransactionsSection(page = 0) {
  const container = document.getElementById('transactions-table');
  const pagination = document.getElementById('transactions-pagination');
  const size = 10;
  container.innerHTML = '<div class="spinner" style="margin:20px auto;"></div>';
  pagination.innerHTML = '';

  try {
    const res = await apiFetch(`/api/transactions?page=${page}&size=${size}`);
    const data = await res.json();

    if (!res.ok || data.error) {
      container.innerHTML = `<div class="alert alert-error">${escapeHtml(data.error || `HTTP ${res.status}`)}</div>`;
      container.appendChild(createCodeBlock(data, true, false));
      return;
    }

    const transactions = data.content || data.transactions || data;
    const totalPages = data.totalPages || 1;
    const currentPage = data.number || page;

    if (!transactions || transactions.length === 0) {
      container.innerHTML = '<div class="empty-state">No transactions found</div>';
      return;
    }

    const table = document.createElement('table');
    table.className = 'table';
    const thead = document.createElement('thead');
    thead.innerHTML = `
      <tr>
        <th>Data</th>
        <th>Descrição</th>
        <th>Valor</th>
        <th>Categoria</th>
        <th>Tipo</th>
        <th>ID</th>
      </tr>`;
    table.appendChild(thead);
    const tbody = document.createElement('tbody');
    transactions.forEach(tx => {
      const row = document.createElement('tr');
      const isIncome = tx.type === 'INCOME';
      const typeClass = isIncome ? 'green' : 'red';
      const sign = isIncome ? '' : '-';
      row.innerHTML = `
        <td>${formatDate(tx.transactionDate || tx.createdAt)}</td>
        <td>${escapeHtml(tx.description || '-')}</td>
        <td style="font-family:var(--font-mono);color:var(--color-accent-${typeClass});">${sign}${tx.amount != null ? formatBRL(Math.abs(tx.amount)) : '-'}</td>
        <td>${escapeHtml(tx.categoryDescription || tx.category || '-')}</td>
        <td>${escapeHtml(tx.typeDescription || tx.type || '-')}</td>
        <td style="font-family:var(--font-mono);font-size:11px;color:var(--color-text-muted);">${escapeHtml(tx.id ? tx.id.substring(0, 8) + '…' : '-')}</td>`;
      tbody.appendChild(row);
    });
    table.appendChild(tbody);
    container.innerHTML = '';
    container.appendChild(table);

    pagination.innerHTML = '';
    if (totalPages > 1) {
      const prevBtn = document.createElement('button');
      prevBtn.className = 'btn btn-secondary';
      prevBtn.textContent = '← Previous';
      prevBtn.disabled = currentPage <= 0;
      prevBtn.onclick = () => renderTransactionsSection(currentPage - 1);

      const pageInfo = document.createElement('span');
      pageInfo.className = 'small text-muted';
      pageInfo.textContent = `Page ${currentPage + 1} of ${totalPages}`;

      const nextBtn = document.createElement('button');
      nextBtn.className = 'btn btn-secondary';
      nextBtn.textContent = 'Next →';
      nextBtn.disabled = currentPage >= totalPages - 1;
      nextBtn.onclick = () => renderTransactionsSection(currentPage + 1);

      pagination.append(prevBtn, pageInfo, nextBtn);
    }
  } catch (err) {
    container.innerHTML = `<div class="alert alert-error">Network error: ${escapeHtml(err.message)}</div>`;
  }
}

async function renderBalanceSection() {
  const display = document.getElementById('balance-display');
  const card = document.getElementById('balance-card');
  setLoading('balance-card', true);

  try {
    const res = await apiFetch('/api/transactions/balance');
    const data = await res.json();

    if (!res.ok || data.error) {
      display.textContent = 'Error';
      display.className = 'balance-display muted';
      const resultArea = document.getElementById('balance-result');
      resultArea.innerHTML = '';
      resultArea.appendChild(createCodeBlock(data, true, false));
      setLoading('balance-card', false);
      return;
    }

    const balance = data.balance != null ? data.balance : data;
    const numericBalance = typeof balance === 'number' ? balance : parseFloat(balance);
    display.textContent = formatBRL(numericBalance);
    display.className = `balance-display ${numericBalance >= 0 ? 'green' : 'red'}`;

    const resultArea = document.getElementById('balance-result');
    resultArea.innerHTML = '';
    resultArea.appendChild(createCodeBlock(data, true, false));
  } catch (err) {
    display.textContent = 'Unreachable';
    display.className = 'balance-display muted';
    const resultArea = document.getElementById('balance-result');
    resultArea.innerHTML = `<div class="alert alert-error">Network error: ${escapeHtml(err.message)}</div>`;
  }
  setLoading('balance-card', false);
}

async function renderSummarySection() {
  const now = new Date();
  const monthSelect = document.getElementById('summary-month');
  const yearSelect = document.getElementById('summary-year');
  const loadBtn = document.getElementById('summary-load-btn');
  const container = document.getElementById('summary-result');

  if (!monthSelect.hasChildNodes()) {
    const months = [
      'Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho',
      'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro'
    ];
    months.forEach((m, i) => {
      const opt = document.createElement('option');
      opt.value = String(i + 1).padStart(2, '0');
      opt.textContent = m;
      if (i === now.getMonth()) opt.selected = true;
      monthSelect.appendChild(opt);
    });
  }

  if (!yearSelect.hasChildNodes()) {
    for (let y = now.getFullYear() - 2; y <= now.getFullYear() + 1; y++) {
      const opt = document.createElement('option');
      opt.value = String(y);
      opt.textContent = String(y);
      if (y === now.getFullYear()) opt.selected = true;
      yearSelect.appendChild(opt);
    }
  }

  async function loadSummary() {
    const month = monthSelect.value;
    const year = yearSelect.value;
    loadBtn.disabled = true;
    loadBtn.textContent = 'Carregando...';
    container.innerHTML = '<div class="spinner" style="margin:20px auto;"></div>';

    try {
      const res = await apiFetch(`/api/transactions/summary/${year}/${month}`);
      const data = await res.json();

      if (!res.ok || data.error) {
        container.innerHTML = `<div class="alert alert-error">${escapeHtml(data.error || `HTTP ${res.status}`)}</div>`;
        container.appendChild(createCodeBlock(data, true, false));
        loadBtn.disabled = false;
        loadBtn.textContent = 'Carregar';
        return;
      }

      const summary = data;
      container.innerHTML = '';

      const cardsDiv = document.createElement('div');
      cardsDiv.className = 'summary-cards';

      function createSummaryCard(label, value, colorClass) {
        const card = document.createElement('div');
        card.className = 'card card-summary';
        card.innerHTML = `
          <div class="card-header">
            <span class="card-title">${escapeHtml(label)}</span>
          </div>
          <div class="card-body">
            <span class="card-value ${colorClass}">${value}</span>
          </div>`;
        return card;
      }

      const income = summary.totalIncome != null ? formatBRL(summary.totalIncome) : formatBRL(0);
      const expense = summary.totalExpense != null ? formatBRL(Math.abs(summary.totalExpense)) : formatBRL(0);
      const net = summary.balance != null ? formatBRL(summary.balance) : formatBRL(0);
      const netClass = summary.balance >= 0 ? 'green' : 'red';

      cardsDiv.appendChild(createSummaryCard('Entradas', income, 'green'));
      cardsDiv.appendChild(createSummaryCard('Saídas', expense, 'red'));
      cardsDiv.appendChild(createSummaryCard('Saldo', net, netClass));

      container.appendChild(cardsDiv);

      if (summary.byCategory && summary.byCategory.length > 0) {
        const expenseCats = summary.byCategory.filter(c => c.totalAmount < 0);
        const incomeCats = summary.byCategory.filter(c => c.totalAmount >= 0);

        if (expenseCats.length > 0) {
          const chartHeader = document.createElement('h3');
          chartHeader.className = 'chart-section-title';
          chartHeader.textContent = 'Despesas por Categoria';
          container.appendChild(chartHeader);
          container.appendChild(createBarChart(expenseCats, 'red'));
        }

        if (incomeCats.length > 0) {
          const chartHeader = document.createElement('h3');
          chartHeader.className = 'chart-section-title';
          chartHeader.textContent = 'Receitas por Categoria';
          container.appendChild(chartHeader);
          container.appendChild(createBarChart(incomeCats, 'green'));
        }
      } else {
        const emptyEl = document.createElement('p');
        emptyEl.className = 'empty-state';
        emptyEl.textContent = 'Nenhuma transação neste período';
        container.appendChild(emptyEl);
      }

      container.appendChild(createCodeBlock(summary, true, false));
    } catch (err) {
      container.innerHTML = `<div class="alert alert-error">Network error: ${escapeHtml(err.message)}</div>`;
    }

    loadBtn.disabled = false;
    loadBtn.textContent = 'Carregar';
  }

  function createBarChart(categories, type) {
    const absAmounts = categories.map(c => ({ ...c, absAmount: Math.abs(c.totalAmount) }));
    const maxAmount = Math.max(...absAmounts.map(c => c.absAmount), 1);

    const container = document.createElement('div');
    container.className = 'chart-bars';

    absAmounts.forEach(cat => {
      const pct = (cat.absAmount / maxAmount) * 100;
      const barRow = document.createElement('div');
      barRow.className = 'chart-row';

      const label = document.createElement('div');
      label.className = 'chart-label';
      label.textContent = cat.categoryDescription || cat.category;

      const barTrack = document.createElement('div');
      barTrack.className = 'chart-track';

      const bar = document.createElement('div');
      bar.className = `chart-bar chart-bar-${type}`;
      bar.style.width = `${Math.max(pct, 4)}%`;

      const value = document.createElement('span');
      value.className = 'chart-value';
      value.textContent = formatBRL(cat.absAmount);

      const count = document.createElement('span');
      count.className = 'chart-count';
      count.textContent = `${cat.transactionCount} ${cat.transactionCount === 1 ? 'transação' : 'transações'}`;

      barTrack.appendChild(bar);
      barRow.appendChild(label);
      barRow.appendChild(barTrack);
      barRow.appendChild(value);
      barRow.appendChild(count);
      container.appendChild(barRow);
    });

    return container;
  }

  loadBtn.onclick = loadSummary;
}

function initConfigSection() {
  const baseUrlInput = document.getElementById('config-base-url');
  const saveBtn = document.getElementById('config-save-btn');
  const testBtn = document.getElementById('config-test-btn');

  baseUrlInput.value = getBaseUrl();

  saveBtn.addEventListener('click', () => {
    const url = baseUrlInput.value.trim();
    if (!url) {
      createToast('Please enter a valid URL', 'warning');
      return;
    }
    setBaseUrl(url);
    createToast('Base URL saved', 'success');
    checkHealth();
  });

  testBtn.addEventListener('click', async () => {
    testBtn.disabled = true;
    testBtn.textContent = 'Testing...';
    const online = await checkHealth();
    createToast(online ? 'Connection successful' : 'Connection failed', online ? 'success' : 'error');
    testBtn.disabled = false;
    testBtn.textContent = 'Test Connection';
  });
}

const SECTION_MAP = {
  '#health': 'section-health',
  '#voice': 'section-voice',
  '#transactions': 'section-transactions',
  '#summary': 'section-summary',
  '#settings': 'section-settings',
};

function navigateTo(hash) {
  if (!hash || hash === '#') hash = '#health';
  if (!SECTION_MAP[hash]) return;
  document.querySelectorAll('.section').forEach(s => s.classList.remove('active'));
  document.querySelectorAll('.sidebar-link').forEach(l => l.classList.remove('active'));

  const section = document.getElementById(SECTION_MAP[hash]);
  if (section) section.classList.add('active');

  const link = document.querySelector(`.sidebar-link[href="${hash}"]`);
  if (link) link.classList.add('active');

  window.location.hash = hash;

  const headerBaseUrl = document.getElementById('headerBaseUrl');
  if (headerBaseUrl) headerBaseUrl.textContent = getBaseUrl();

  if (hash === '#health') renderHealthSection();
  else if (hash === '#transactions') {
    renderBalanceSection();
    renderTransactionsSection(0);
  } else if (hash === '#summary') renderSummarySection();
}

window.addEventListener('hashchange', () => {
  navigateTo(window.location.hash || '#health');
});

document.addEventListener('DOMContentLoaded', () => {
  initVoiceSection();
  initConfigSection();
  checkHealth();

  const hash = window.location.hash || '#health';
  navigateTo(hash);
});
