/*
 * Escáner de código de barras con cámara, reutilizable en cualquier página.
 * Requiere que la página incluya antes:
 *   <script src="https://unpkg.com/html5-qrcode@2.3.8/html5-qrcode.min.js"></script>
 *   <script src="/scanner.js"></script>
 * Uso: mrkAbrirScanner(function(codigo) { ... }); mrkCerrarScanner();
 */
(function () {
    let html5QrCode = null;
    let scannerActivo = false;
    let onDecodedCallback = null;

    function inyectarModal() {
        if (document.getElementById('mrkModalScanner')) return;
        const wrapper = document.createElement('div');
        wrapper.innerHTML = `
<div id="mrkModalScanner" class="modal-overlay">
    <div class="modal-content" style="max-width:480px; padding:20px;">
        <h3 style="margin:0 0 16px; font-size:16px; font-weight:700; color:var(--text); text-align:center;">📷 Escanear Código de Barras</h3>
        <div id="mrkScannerContainer" style="width:100%; border-radius:12px; overflow:hidden; background:#000; min-height:260px;"></div>
        <p style="text-align:center; font-size:12.5px; color:var(--text-muted); margin-top:12px;">Apunta la cámara al código de barras del producto.</p>
        <p id="mrkScannerError" style="display:none; text-align:center; font-size:13px; color:#f87171; background:rgba(220,38,38,0.1); border:1px solid rgba(220,38,38,0.2); border-radius:8px; padding:12px; margin-top:12px;"></p>
        <div style="display:flex; gap:10px; margin-top:16px;">
            <button type="button" class="btn btn-secondary" style="flex:1; justify-content:center;" onclick="mrkCerrarScanner()">Cerrar</button>
        </div>
    </div>
</div>`;
        document.body.appendChild(wrapper.firstElementChild);
    }

    function onCodigoEscaneado(decodedText) {
        if (!scannerActivo) return;
        scannerActivo = false;
        html5QrCode.stop().catch(() => {}).finally(() => {
            const modal = document.getElementById('mrkModalScanner');
            if (modal) modal.classList.remove('active');
            if (onDecodedCallback) onDecodedCallback(decodedText);
        });
    }

    window.mrkAbrirScanner = async function (onDecoded) {
        if (typeof Html5Qrcode === 'undefined') {
            alert("No se pudo cargar el módulo de cámara. Revisa tu conexión a internet.");
            return;
        }
        inyectarModal();
        onDecodedCallback = onDecoded;

        const errorEl = document.getElementById('mrkScannerError');
        errorEl.style.display = 'none';
        document.getElementById('mrkModalScanner').classList.add('active');

        try {
            if (!html5QrCode) html5QrCode = new Html5Qrcode("mrkScannerContainer");
            const config = {
                fps: 10,
                qrbox: { width: 260, height: 140 },
                formatsToSupport: [
                    Html5QrcodeSupportedFormats.EAN_13,
                    Html5QrcodeSupportedFormats.EAN_8,
                    Html5QrcodeSupportedFormats.UPC_A,
                    Html5QrcodeSupportedFormats.UPC_E,
                    Html5QrcodeSupportedFormats.CODE_128,
                    Html5QrcodeSupportedFormats.CODE_39,
                    Html5QrcodeSupportedFormats.CODABAR,
                    Html5QrcodeSupportedFormats.ITF
                ]
            };
            await html5QrCode.start({ facingMode: "environment" }, config, onCodigoEscaneado, () => {});
            scannerActivo = true;
        } catch (e) {
            console.error(e);
            errorEl.textContent = "No se pudo acceder a la cámara. Revisa los permisos del navegador.";
            errorEl.style.display = 'block';
        }
    };

    window.mrkCerrarScanner = async function () {
        const modal = document.getElementById('mrkModalScanner');
        if (modal) modal.classList.remove('active');
        if (html5QrCode && scannerActivo) {
            scannerActivo = false;
            try { await html5QrCode.stop(); } catch (e) {}
        }
    };
})();
