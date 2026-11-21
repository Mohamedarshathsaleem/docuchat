import { useRef, useState } from 'react';
import { api, DocumentInfo } from '../api';

const MAX_BYTES = 5 * 1024 * 1024;

interface Props {
  onUploaded: (doc: DocumentInfo) => void;
}

export default function UploadPanel({ onUploaded }: Props) {
  const input = useRef<HTMLInputElement>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [dragging, setDragging] = useState(false);

  async function handle(file: File | undefined) {
    if (!file) return;
    setError(null);
    if (file.type !== 'application/pdf' && !file.name.toLowerCase().endsWith('.pdf')) {
      setError('Please choose a PDF file.');
      return;
    }
    if (file.size > MAX_BYTES) {
      setError('PDFs must be 5 MB or smaller.');
      return;
    }
    setBusy(true);
    try {
      onUploaded(await api.upload(file));
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
      if (input.current) input.current.value = '';
    }
  }

  return (
    <div
      className={`dropzone${dragging ? ' dragging' : ''}`}
      onDragOver={(e) => {
        e.preventDefault();
        setDragging(true);
      }}
      onDragLeave={() => setDragging(false)}
      onDrop={(e) => {
        e.preventDefault();
        setDragging(false);
        handle(e.dataTransfer.files[0]);
      }}
    >
      <label htmlFor="pdf-input" className="dropzone-label">
        {busy ? 'Reading and embedding…' : 'Drop a PDF here or click to upload'}
      </label>
      <input
        id="pdf-input"
        ref={input}
        type="file"
        accept="application/pdf,.pdf"
        disabled={busy}
        onChange={(e) => handle(e.target.files?.[0])}
      />
      {error && (
        <p role="alert" className="error">
          {error}
        </p>
      )}
    </div>
  );
}
