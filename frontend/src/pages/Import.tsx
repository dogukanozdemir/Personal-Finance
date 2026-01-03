import { useState } from 'react';
import { Upload, CheckCircle, XCircle } from 'lucide-react';
import { importAPI } from '../utils/api';

const Import = () => {
  const [file, setFile] = useState<File | null>(null);
  const [result, setResult] = useState<any>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  
  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      setFile(e.target.files[0]);
      setResult(null);
      setError(null);
    }
  };
  
  const handleUpload = async () => {
    if (!file) return;
    
    setLoading(true);
    setError(null);
    
    try {
      const response = await importAPI.uploadFile(file);
      setResult(response.data);
    } catch (err: any) {
      setError(err.response?.data || 'Failed to import file');
    } finally {
      setLoading(false);
    }
  };
  
  return (
    <div className="space-y-6">
      <h1 className="text-3xl font-bold">Import Data</h1>
      
      {/* Upload Area */}
      <div className="card">
        <div className="border-2 border-dashed border-gray-700 rounded-lg p-12 text-center hover:border-primary transition-colors">
          <Upload size={48} className="mx-auto mb-4 text-text-muted" />
          <h3 className="text-lg font-semibold mb-2">
            {file ? file.name : 'Upload Bank Statement'}
          </h3>
          <p className="text-text-muted mb-4">
            Supports Excel (.xlsx) and CSV files
          </p>
          <input
            type="file"
            accept=".xlsx,.xls,.csv"
            onChange={handleFileChange}
            className="hidden"
            id="file-upload"
          />
          <label htmlFor="file-upload" className="btn-primary inline-block cursor-pointer">
            {file ? 'Change File' : 'Select File'}
          </label>
        </div>
        
        {file && (
          <div className="mt-6 flex justify-center">
            <button
              onClick={handleUpload}
              disabled={loading}
              className="btn-primary"
            >
              {loading ? 'Importing...' : 'Import Transactions'}
            </button>
          </div>
        )}
      </div>
      
      {/* Import Result */}
      {result && (
        <div className="card border-l-4 border-success">
          <div className="flex items-start justify-between">
            <div className="flex-1">
              <div className="flex items-center space-x-2 mb-4">
                <CheckCircle className="text-success" size={24} />
                <h3 className="text-xl font-semibold">Import Successful</h3>
              </div>
              
              <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                <div>
                  <p className="text-text-muted text-sm">Total Rows</p>
                  <p className="text-2xl font-bold">{result.totalRows}</p>
                </div>
                <div>
                  <p className="text-text-muted text-sm">New Transactions</p>
                  <p className="text-2xl font-bold text-success">{result.newTransactions}</p>
                </div>
                <div>
                  <p className="text-text-muted text-sm">Duplicates Skipped</p>
                  <p className="text-2xl font-bold text-accent">{result.duplicates}</p>
                </div>
                <div>
                  <p className="text-text-muted text-sm">Date Range</p>
                  <p className="text-sm">
                    {result.dateRangeStart} to {result.dateRangeEnd}
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
      
      {/* Error */}
      {error && (
        <div className="card border-l-4 border-danger">
          <div className="flex items-center space-x-2">
            <XCircle className="text-danger" size={24} />
            <div>
              <h3 className="text-xl font-semibold">Import Failed</h3>
              <p className="text-text-muted">{error}</p>
            </div>
          </div>
        </div>
      )}
      
      {/* Instructions */}
      <div className="card">
        <h3 className="text-xl font-semibold mb-4">How to Import</h3>
        <ol className="space-y-2 list-decimal list-inside text-text-muted">
          <li>Download your bank statement from your online banking portal</li>
          <li>Make sure it's in Excel (.xlsx) or CSV format</li>
          <li>Upload the file using the button above</li>
          <li>The system will automatically detect duplicates and import only new transactions</li>
          <li>You can re-upload the same file multiple times - duplicates will be skipped</li>
        </ol>
      </div>
    </div>
  );
};

export default Import;

