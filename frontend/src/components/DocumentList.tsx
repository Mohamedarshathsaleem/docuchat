import { DocumentInfo } from '../api';

interface Props {
  documents: DocumentInfo[];
  selectedId?: string;
  onSelect: (doc: DocumentInfo) => void;
  onDelete: (doc: DocumentInfo) => void;
}

export default function DocumentList({ documents, selectedId, onSelect, onDelete }: Props) {
  if (documents.length === 0) return null;
  return (
    <ul className="doc-list" aria-label="Uploaded documents">
      {documents.map((doc) => (
        <li key={doc.id} className={doc.id === selectedId ? 'active' : undefined}>
          <button className="doc-select" onClick={() => onSelect(doc)}>
            <span className="doc-name">{doc.filename}</span>
            <span className="doc-meta">
              {doc.pages} pages · {doc.chunks} chunks
            </span>
          </button>
          <button className="doc-delete" aria-label={`Delete ${doc.filename}`} onClick={() => onDelete(doc)}>
            ×
          </button>
        </li>
      ))}
    </ul>
  );
}
