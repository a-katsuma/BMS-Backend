// --- 1. 新規登録時のボタン表示制御 ---
document.addEventListener('DOMContentLoaded', function() {
    updatePreviewButtonVisibility();
});

function enablePreviewButton() {
    updatePreviewButtonVisibility();
}

function updatePreviewButtonVisibility() {
    const fileInput = document.getElementById('file-input');
    const previewBtn = document.getElementById('preview-btn');
    if (fileInput && previewBtn) {
        // ファイルが選択されている時のみボタンを表示
        previewBtn.style.display = (fileInput.files.length > 0) ? 'inline-block' : 'none';
    }
}

// --- 2. モーダル操作：新規・編集共通 ---
function openModal() {
    const pdfModal = document.getElementById('pdf-modal');
    const fileInput = document.getElementById('file-input');
    // モーダル表示用の要素（iframeを推奨）
    const modalPreview = document.getElementById('pdf-modal-preview');

    if (!pdfModal || !modalPreview) return;

    // A. 新規ファイル選択時：FileReaderで読み込む
    if (fileInput && fileInput.files.length > 0) {
        const file = fileInput.files[0];
        if (file.type === "application/pdf") {
            const reader = new FileReader();
            reader.onload = function(e) {
                modalPreview.src = e.target.result;
                pdfModal.style.display = 'flex';
            };
            reader.readAsDataURL(file);
        }
    }
    // B. 既存ファイルの編集時：現在表示中のパスをモーダルへ引き継ぐ
    else {
        const currentEmbed = document.getElementById('pdf-preview');
        if (currentEmbed && currentEmbed.src) {
            modalPreview.src = currentEmbed.src;
            pdfModal.style.display = 'flex';
        }
    }
}

function closeModal() {
    const pdfModal = document.getElementById('pdf-modal');
    if (pdfModal) {
        pdfModal.style.display = 'none';
    }
}