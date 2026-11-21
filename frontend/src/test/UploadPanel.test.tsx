import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import UploadPanel from '../components/UploadPanel';

afterEach(() => vi.unstubAllGlobals());

describe('UploadPanel', () => {
  it('rejects non-PDF files without calling the API', async () => {
    const fetchMock = vi.fn();
    vi.stubGlobal('fetch', fetchMock);

    render(<UploadPanel onUploaded={vi.fn()} />);
    const input = screen.getByLabelText(/upload/i);
    await userEvent.upload(input, new File(['hello'], 'notes.txt', { type: 'text/plain' }), { applyAccept: false });

    expect(screen.getByRole('alert')).toHaveTextContent('Please choose a PDF file.');
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('uploads a PDF and reports the indexed document', async () => {
    const uploaded = { id: 'd1', filename: 'paper.pdf', pages: 4, chunks: 9, uploadedAt: '2026-11-21T00:00:00Z' };
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true, status: 201, json: () => Promise.resolve(uploaded) }));
    const onUploaded = vi.fn();

    render(<UploadPanel onUploaded={onUploaded} />);
    await userEvent.upload(screen.getByLabelText(/upload/i), new File(['%PDF-1.7'], 'paper.pdf', { type: 'application/pdf' }));

    await vi.waitFor(() => expect(onUploaded).toHaveBeenCalledWith(uploaded));
  });
});
