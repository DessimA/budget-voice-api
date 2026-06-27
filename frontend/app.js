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

function initCommandSection() {
  const uploadZone = document.getElementById('command-upload-zone');
  const fileInput = document.getElementById('command-file-input');
  const preview = document.getElementById('command-preview');
  const sendBtn = document.getElementById('command-send-btn');
  const micBtn = document.getElementById('command-mic-btn');
  let selectedFile = null;

  function updatePreview(file) {
    selectedFile = file;
    if (file) {
      uploadZone.classList.add('has-file');
      preview.innerHTML = `
        <div class="upload-preview">
          <span>🎵</span>
          <span class="upload-name">${escapeHtml(file.name)}</span>
          <span class="upload-size">(${(file.size / 1024).toFixed(1)} KB)</span>
          <span class="upload-remove" style="cursor:pointer;color:var(--color-accent-red);">✕</span>
        </div>`;
      preview.querySelector('.upload-remove').onclick = (e) => {
        e.stopPropagation();
        clearFile();
      };
    } else {
      uploadZone.classList.remove('has-file');
      preview.innerHTML = `<div class="upload-placeholder"><p>Click or drag an audio file here (MP3, WAV, OGG, M4A)</p></div>`;
    }
  }

  function clearFile() {
    fileInput.value = '';
    updatePreview(null);
  }

  uploadZone.addEventListener('click', () => fileInput.click());

  uploadZone.addEventListener('dragover', (e) => {
    e.preventDefault();
    uploadZone.classList.add('drag-over');
  });

  uploadZone.addEventListener('dragleave', () => {
    uploadZone.classList.remove('drag-over');
  });

  uploadZone.addEventListener('drop', (e) => {
    e.preventDefault();
    uploadZone.classList.remove('drag-over');
    const file = e.dataTransfer.files[0];
    if (file) updatePreview(file);
  });

  fileInput.addEventListener('change', () => {
    if (fileInput.files[0]) updatePreview(fileInput.files[0]);
  });

  sendBtn.addEventListener('click', async () => {
    if (!selectedFile) {
      createToast('Please select an audio file first', 'warning');
      return;
    }
    sendBtn.disabled = true;
    sendBtn.textContent = 'Sending...';
    document.getElementById('command-send-result').innerHTML = '';

    try {
      const formData = new FormData();
      formData.append('audio', selectedFile);
      const res = await fetch(getBaseUrl().replace(/\/+$/, '') + '/api/voice/command', {
        method: 'POST',
        body: formData,
      });
      const data = await res.json();
      const container = document.getElementById('command-send-result');
      const statusInfo = statusLabel(res.status);
      const badge = createBadge(statusInfo.text, statusInfo.type);
      const resultPanel = document.createElement('div');
      resultPanel.className = 'result-panel';
      resultPanel.appendChild(badge);
      if (data.transcribedText) {
        const tLabel = document.createElement('h3');
        tLabel.textContent = 'Transcrição';
        const tText = document.createElement('p');
        tText.className = 'result-text';
        tText.textContent = data.transcribedText;
        resultPanel.appendChild(tLabel);
        resultPanel.appendChild(tText);
      }
      if (data.aiResponse) {
        const rLabel = document.createElement('h3');
        rLabel.textContent = 'Resposta';
        const rText = document.createElement('p');
        rText.className = 'result-text';
        rText.textContent = data.aiResponse;
        resultPanel.appendChild(rLabel);
        resultPanel.appendChild(rText);
      }
      container.appendChild(resultPanel);
      container.appendChild(createCodeBlock(data, true, false));
      if (data.status === 'success') {
        createToast('Comando processado com sucesso', 'success');
      } else {
        createToast('Comando processado com ressalvas', 'warning');
      }
    } catch (err) {
      const container = document.getElementById('command-send-result');
      const msg = err.name === 'TypeError' && err.message.includes('Failed to fetch')
        ? `Servidor inacessível em ${getBaseUrl()}`
        : err.message;
      container.innerHTML = `<div class="alert alert-error">${escapeHtml(msg)}</div>`;
      createToast('Falha na requisição', 'error');
    }

    sendBtn.disabled = false;
    sendBtn.textContent = 'Send Command';
  });

  if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
    let mediaRecorder = null;
    let audioChunks = [];
    let isRecording = false;

    micBtn.addEventListener('click', async () => {
      if (isRecording) {
        mediaRecorder.stop();
        isRecording = false;
        micBtn.textContent = '🎤 Record';
        return;
      }

      try {
        const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
        mediaRecorder = new MediaRecorder(stream, { mimeType: MediaRecorder.isTypeSupported('audio/webm') ? 'audio/webm' : 'audio/mp4' });
        audioChunks = [];

        mediaRecorder.ondataavailable = (e) => {
          if (e.data.size > 0) audioChunks.push(e.data);
        };

        mediaRecorder.onstop = () => {
          const blob = new Blob(audioChunks, { type: mediaRecorder.mimeType });
          const file = new File([blob], `recording.${mediaRecorder.mimeType.includes('mp4') ? 'mp4' : 'webm'}`, { type: mediaRecorder.mimeType });
          updatePreview(file);
          stream.getTracks().forEach(t => t.stop());
        };

        mediaRecorder.start();
        isRecording = true;
        micBtn.textContent = '⏹ Stop';
        createToast('Recording...', 'info');
      } catch {
        createToast('Microphone access denied', 'error');
      }
    });
  } else {
    micBtn.style.display = 'none';
  }
}

async function initAudioSection() {
  const uploadZone = document.getElementById('audio-upload-zone');
  const fileInput = document.getElementById('audio-file-input');
  const preview = document.getElementById('audio-preview');
  const sendBtn = document.getElementById('audio-send-btn');
  const result = document.getElementById('audio-result');
  let selectedFile = null;

  function updatePreview(file) {
    selectedFile = file;
    if (file) {
      uploadZone.classList.add('has-file');
      preview.innerHTML = `
        <div class="upload-preview">
          <span>🎵</span>
          <span class="upload-name">${escapeHtml(file.name)}</span>
          <span class="upload-size">(${(file.size / 1024).toFixed(1)} KB)</span>
          <span class="upload-remove" style="cursor:pointer;color:var(--color-accent-red);">✕</span>
        </div>`;
      preview.querySelector('.upload-remove').onclick = (e) => {
        e.stopPropagation();
        fileInput.value = '';
        selectedFile = null;
        updatePreview(null);
      };
    } else {
      uploadZone.classList.remove('has-file');
      preview.innerHTML = `<div class="upload-placeholder"><p>Click or drag to upload an audio command</p></div>`;
    }
  }

  uploadZone.addEventListener('click', () => fileInput.click());
  uploadZone.addEventListener('dragover', (e) => { e.preventDefault(); uploadZone.classList.add('drag-over'); });
  uploadZone.addEventListener('dragleave', () => uploadZone.classList.remove('drag-over'));
  uploadZone.addEventListener('drop', (e) => {
    e.preventDefault();
    uploadZone.classList.remove('drag-over');
    if (e.dataTransfer.files[0]) updatePreview(e.dataTransfer.files[0]);
  });
  fileInput.addEventListener('change', () => { if (fileInput.files[0]) updatePreview(fileInput.files[0]); });

  sendBtn.addEventListener('click', async () => {
    if (!selectedFile) {
      createToast('Select an audio file first', 'warning');
      return;
    }
    sendBtn.disabled = true;
    sendBtn.textContent = 'Processing...';
    result.innerHTML = '<div class="spinner" style="margin:20px auto;"></div>';

    try {
      const formData = new FormData();
      formData.append('audio', selectedFile);
      const res = await fetch(getBaseUrl().replace(/\/+$/, '') + '/api/voice/command/audio', { method: 'POST', body: formData });

      if (!res.ok) {
        const contentType = res.headers.get('content-type') || '';
        let errData;
        if (contentType.includes('json')) {
          errData = await res.json().catch(() => ({ error: res.statusText }));
        } else {
          const text = await res.text().catch(() => res.statusText);
          errData = { error: text };
        }
        result.innerHTML = '';
        const alert = document.createElement('div');
        alert.className = 'alert alert-error';
        const errorMsg = errData.error || `HTTP ${res.status} ${res.statusText}`;
        alert.textContent = errorMsg;
        result.appendChild(alert);
        result.appendChild(createCodeBlock(errData, true, false));
        sendBtn.disabled = false;
        sendBtn.textContent = 'Get Audio Response';
        return;
      }

      const blob = await res.blob();
      const audioUrl = URL.createObjectURL(blob);
      result.innerHTML = '';

      const audioEl = document.createElement('audio');
      audioEl.className = 'audio-player';
      audioEl.controls = true;
      audioEl.src = audioUrl;
      result.appendChild(audioEl);

      const info = document.createElement('p');
      info.className = 'small text-muted';
      info.style.marginTop = '8px';
      info.textContent = `Audio response (${(blob.size / 1024).toFixed(1)} KB)`;
      result.appendChild(info);

      createToast('Audio response received', 'success');
    } catch (err) {
      const msg = err.name === 'TypeError' && err.message.includes('Failed to fetch')
        ? `Servidor inacessível em ${getBaseUrl()}`
        : err.message;
      result.innerHTML = `<div class="alert alert-error">${escapeHtml(msg)}</div>`;
      createToast('Falha na requisição de áudio', 'error');
    }

    sendBtn.disabled = false;
    sendBtn.textContent = 'Get Audio Response';
  });
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

    if (data.period) {
      const periodEl = document.getElementById('balance-period');
      if (periodEl) periodEl.textContent = `Period: ${data.period}`;
    }

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
    loadBtn.textContent = 'Loading...';
    container.innerHTML = '<div class="spinner" style="margin:20px auto;"></div>';

    try {
      const res = await apiFetch(`/api/transactions/summary/${year}/${month}`);
      const data = await res.json();

      if (!res.ok || data.error) {
        container.innerHTML = `<div class="alert alert-error">${escapeHtml(data.error || `HTTP ${res.status}`)}</div>`;
        container.appendChild(createCodeBlock(data, true, false));
        loadBtn.disabled = false;
        loadBtn.textContent = 'Load Summary';
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
        const catHeader = document.createElement('h3');
        catHeader.className = 'card-title';
        catHeader.style.marginTop = '16px';
        catHeader.style.marginBottom = '8px';
        catHeader.textContent = 'Por Categoria';
        container.appendChild(catHeader);

        const table = document.createElement('table');
        table.className = 'table';
        table.innerHTML = `
          <thead><tr>
            <th>Categoria</th>
            <th>Total</th>
            <th>Transações</th>
          </tr></thead>`;
        const tbody = document.createElement('tbody');
        summary.byCategory.forEach(cat => {
          const row = document.createElement('tr');
          row.innerHTML = `
            <td>${escapeHtml(cat.categoryDescription || cat.category)}</td>
            <td style="font-family:var(--font-mono);color:var(--color-accent-${cat.totalAmount >= 0 ? 'green' : 'red'});">${formatBRL(cat.totalAmount)}</td>
            <td>${cat.transactionCount}</td>`;
          tbody.appendChild(row);
        });
        table.appendChild(tbody);
        container.appendChild(table);
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
    loadBtn.textContent = 'Load Summary';
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
  '#voice-text': 'section-voice-text',
  '#voice-audio': 'section-voice-audio',
  '#transactions': 'section-transactions',
  '#balance': 'section-balance',
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

  if (hash === '#health') renderHealthSection();
  else if (hash === '#transactions') renderTransactionsSection(0);
  else if (hash === '#balance') renderBalanceSection();
  else if (hash === '#summary') renderSummarySection();
}

window.addEventListener('hashchange', () => {
  navigateTo(window.location.hash || '#health');
});

document.addEventListener('DOMContentLoaded', () => {
  initCommandSection();
  initAudioSection();
  initConfigSection();
  checkHealth();

  const hash = window.location.hash || '#health';
  navigateTo(hash);
});
