import { useEffect, useState } from 'react';
import { api, DocumentInfo } from './api';
import UploadPanel from './components/UploadPanel';
import DocumentList from './components/DocumentList';
import ChatPanel from './components/ChatPanel';

export default function App() {
  const [documents, setDocuments] = useState<DocumentInfo[]>([]);
  const [selected, setSelected] = useState<DocumentInfo | null>(null);

  useEffect(() => {
    api.listDocuments().then(setDocuments).catch(() => setDocuments([]));
  }, []);

  const onUploaded = (doc: DocumentInfo) => {
    setDocuments((docs) => [doc, ...docs]);
    setSelected(doc);
  };

  const onDelete = async (doc: DocumentInfo) => {
    await api.remove(doc.id);
    setDocuments((docs) => docs.filter((d) => d.id !== doc.id));
    if (selected?.id === doc.id) setSelected(null);
  };

  return (
    <div className="shell">
      <aside className="sidebar">
        <header className="brand">
          <span className="brand-mark" aria-hidden>¶</span>
          <div>
            <h1>DocuChat</h1>
            <p>Ask a PDF anything. Every answer cites its pages.</p>
          </div>
        </header>
        <UploadPanel onUploaded={onUploaded} />
        <DocumentList documents={documents} selectedId={selected?.id} onSelect={setSelected} onDelete={onDelete} />
        <footer className="sidebar-foot">
          Spring Boot · Spring AI · Gemini · React
          <br />
          Uploads are deleted after 24 hours.
        </footer>
      </aside>
      <main className="main">
        {selected ? (
          <ChatPanel key={selected.id} document={selected} />
        ) : (
          <div className="empty">
            <h2>Upload a PDF to begin</h2>
            <p>Try a lecture handout, a policy, a research paper or a product manual (text PDFs up to 5 MB / 50 pages).</p>
          </div>
        )}
      </main>
    </div>
  );
}
