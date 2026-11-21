export interface DocumentInfo {
  id: string;
  filename: string;
  pages: number;
  chunks: number;
  uploadedAt: string;
}

export interface Source {
  page: number;
  excerpt: string;
  score: number;
}

export interface ChatAnswer {
  answer: string;
  sources: Source[];
}

async function handle<T>(response: Response): Promise<T> {
  if (!response.ok) {
    let message = `Request failed (${response.status})`;
    try {
      const problem = await response.json();
      if (problem?.detail) message = problem.detail;
    } catch {
      /* not JSON */
    }
    throw new Error(message);
  }
  return response.status === 204 ? (undefined as T) : response.json();
}

export const api = {
  listDocuments: () => fetch('/api/documents').then((r) => handle<DocumentInfo[]>(r)),

  upload: (file: File) => {
    const body = new FormData();
    body.append('file', file);
    return fetch('/api/documents', { method: 'POST', body }).then((r) => handle<DocumentInfo>(r));
  },

  remove: (id: string) => fetch(`/api/documents/${id}`, { method: 'DELETE' }).then((r) => handle<void>(r)),

  ask: (documentId: string, question: string) =>
    fetch('/api/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ documentId, question }),
    }).then((r) => handle<ChatAnswer>(r)),
};
