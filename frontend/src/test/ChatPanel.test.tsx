import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import ChatPanel from '../components/ChatPanel';

const doc = { id: 'doc-1', filename: 'policy.pdf', pages: 3, chunks: 7, uploadedAt: '2026-11-21T00:00:00Z' };

function mockFetch(status: number, body: unknown) {
  const fetchMock = vi.fn().mockResolvedValue({
    ok: status < 400,
    status,
    json: () => Promise.resolve(body),
  });
  vi.stubGlobal('fetch', fetchMock);
  return fetchMock;
}

afterEach(() => vi.unstubAllGlobals());

describe('ChatPanel', () => {
  it('sends the question for the selected document and shows the cited answer', async () => {
    const fetchMock = mockFetch(200, {
      answer: 'Refunds are allowed within 30 days [p. 2].',
      sources: [{ page: 2, excerpt: 'Refunds within 30 days of purchase', score: 0.81 }],
    });

    render(<ChatPanel document={doc} />);
    await userEvent.type(screen.getByLabelText('Your question'), 'What is the refund policy?');
    await userEvent.click(screen.getByRole('button', { name: 'Ask' }));

    expect(await screen.findByText('Refunds are allowed within 30 days [p. 2].')).toBeInTheDocument();
    expect(screen.getByText(/1 source · pages 2/)).toBeInTheDocument();
    expect(screen.getByText('81% match')).toBeInTheDocument();

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe('/api/chat');
    expect(JSON.parse(init.body)).toEqual({ documentId: 'doc-1', question: 'What is the refund policy?' });
  });

  it('shows the API problem detail when a request fails', async () => {
    mockFetch(429, { detail: 'Rate limit reached (20 requests per minute). Please wait a moment.' });

    render(<ChatPanel document={doc} />);
    await userEvent.click(screen.getByRole('button', { name: 'What are the key dates or deadlines?' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Rate limit reached');
  });

  it('disables Ask until a question is typed', () => {
    render(<ChatPanel document={doc} />);
    expect(screen.getByRole('button', { name: 'Ask' })).toBeDisabled();
  });
});
