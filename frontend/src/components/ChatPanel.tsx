import { FormEvent, useEffect, useRef, useState } from 'react';
import { api, DocumentInfo, Source } from '../api';

interface Turn {
  question: string;
  answer?: string;
  sources?: Source[];
  error?: string;
}

const SUGGESTIONS = ['Summarise this document in 5 bullet points', 'What are the key dates or deadlines?', 'What does it say about costs?'];

export default function ChatPanel({ document }: { document: DocumentInfo }) {
  const [turns, setTurns] = useState<Turn[]>([]);
  const [question, setQuestion] = useState('');
  const [busy, setBusy] = useState(false);
  const bottom = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottom.current?.scrollIntoView?.({ behavior: 'smooth' });
  }, [turns]);

  async function ask(text: string) {
    const q = text.trim();
    if (!q || busy) return;
    setQuestion('');
    setBusy(true);
    setTurns((t) => [...t, { question: q }]);
    try {
      const result = await api.ask(document.id, q);
      setTurns((t) => t.map((turn, i) => (i === t.length - 1 ? { ...turn, ...result } : turn)));
    } catch (e) {
      setTurns((t) => t.map((turn, i) => (i === t.length - 1 ? { ...turn, error: (e as Error).message } : turn)));
    } finally {
      setBusy(false);
    }
  }

  const onSubmit = (e: FormEvent) => {
    e.preventDefault();
    ask(question);
  };

  return (
    <section className="chat" aria-label={`Chat about ${document.filename}`}>
      <header className="chat-head">
        <h2>{document.filename}</h2>
        <span>
          {document.pages} pages · {document.chunks} chunks indexed
        </span>
      </header>

      <div className="turns">
        {turns.length === 0 && (
          <div className="suggestions">
            {SUGGESTIONS.map((s) => (
              <button key={s} onClick={() => ask(s)}>
                {s}
              </button>
            ))}
          </div>
        )}
        {turns.map((turn, i) => (
          <article key={i} className="turn">
            <p className="question">{turn.question}</p>
            {turn.error && (
              <p role="alert" className="error">
                {turn.error}
              </p>
            )}
            {!turn.answer && !turn.error && <p className="thinking">Searching the document…</p>}
            {turn.answer && <p className="answer">{turn.answer}</p>}
            {turn.sources && turn.sources.length > 0 && (
              <details className="sources">
                <summary>
                  {turn.sources.length} source{turn.sources.length > 1 ? 's' : ''} · pages{' '}
                  {[...new Set(turn.sources.map((s) => s.page))].sort((a, b) => a - b).join(', ')}
                </summary>
                <ol>
                  {turn.sources.map((s, j) => (
                    <li key={j}>
                      <strong>p. {s.page}</strong> <span className="score">{Math.round(s.score * 100)}% match</span>
                      <blockquote>{s.excerpt}</blockquote>
                    </li>
                  ))}
                </ol>
              </details>
            )}
          </article>
        ))}
        <div ref={bottom} />
      </div>

      <form className="ask" onSubmit={onSubmit}>
        <label htmlFor="question" className="visually-hidden">
          Your question
        </label>
        <input
          id="question"
          value={question}
          maxLength={1000}
          placeholder="Ask about this document…"
          onChange={(e) => setQuestion(e.target.value)}
          disabled={busy}
        />
        <button type="submit" disabled={busy || !question.trim()}>
          Ask
        </button>
      </form>
    </section>
  );
}
