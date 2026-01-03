import { useState } from 'react';
import { Upload, CheckCircle, XCircle, FileText, Loader } from 'lucide-react';
import { importAPI } from '../utils/api';

const Import = () => {
  const [files, setFiles] = useState<File[]>([]);
  const [result, setResult] = useState<any>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [processingStatus, setProcessingStatus] = useState<{[key: string]: 'pending' | 'processing' | 'success' | 'error'}>({});
  
  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      const fileList = Array.from(e.target.files);
      setFiles(fileList);
      setResult(null);
      setError(null);
      
      // Initialize status for all files
      const status: {[key: string]: 'pending' | 'processing' | 'success' | 'error'} = {};
      fileList.forEach(file => {
        status[file.name] = 'pending';
      });
      setProcessingStatus(status);
    }
  };
  
  const handleUpload = async () => {
    if (files.length === 0) return;
    
    setLoading(true);
    setError(null);
    
    try {
      const response = await importAPI.uploadFiles(files, (filename, status) => {
        setProcessingStatus(prev => ({...prev, [filename]: status}));
      });
      setResult(response.data);
    } catch (err: any) {
      setError(err.response?.data || 'Failed to import files');
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
            {files.length > 0 ? `${files.length} file${files.length > 1 ? 's' : ''} selected` : 'Upload Bank Statements'}
          </h3>
          <p className="text-text-muted mb-4">
            Supports Excel (.xlsx, .xls) - Select multiple files for batch import
          </p>
          <input
            type="file"
            accept=".xlsx,.xls"
            onChange={handleFileChange}
            className="hidden"
            id="file-upload"
            multiple
          />
          <label htmlFor="file-upload" className="btn-primary inline-block cursor-pointer">
            {files.length > 0 ? 'Change Files' : 'Select Files'}
          </label>
        </div>
        
        {/* File List */}
        {files.length > 0 && (
          <div className="mt-6">
            <h4 className="text-sm font-semibold mb-3 text-text-muted">Selected Files:</h4>
            <div className="space-y-2">
              {files.map((file, index) => (
                <div key={index} className="flex items-center justify-between p-3 bg-background rounded-lg border border-gray-700">
                  <div className="flex items-center space-x-3">
                    <FileText size={20} className="text-primary" />
                    <div>
                      <p className="text-sm font-medium">{file.name}</p>
                      <p className="text-xs text-text-muted">{(file.size / 1024).toFixed(2)} KB</p>
                    </div>
                  </div>
                  {processingStatus[file.name] && (
                    <div className="flex items-center space-x-2">
                      {processingStatus[file.name] === 'pending' && (
                        <span className="text-xs text-text-muted">Pending</span>
                      )}
                      {processingStatus[file.name] === 'processing' && (
                        <>
                          <Loader size={16} className="animate-spin text-primary" />
                          <span className="text-xs text-primary">Processing...</span>
                        </>
                      )}
                      {processingStatus[file.name] === 'success' && (
                        <>
                          <CheckCircle size={16} className="text-success" />
                          <span className="text-xs text-success">Done</span>
                        </>
                      )}
                      {processingStatus[file.name] === 'error' && (
                        <>
                          <XCircle size={16} className="text-danger" />
                          <span className="text-xs text-danger">Failed</span>
                        </>
                      )}
                    </div>
                  )}
                </div>
              ))}
            </div>
            
            <div className="mt-6 flex justify-center">
              <button
                onClick={handleUpload}
                disabled={loading}
                className="btn-primary"
              >
                {loading ? 'Importing...' : `Import ${files.length} File${files.length > 1 ? 's' : ''}`}
              </button>
            </div>
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
          <li>Download your bank statements from your online banking portal</li>
          <li>Make sure they're in Excel format (.xlsx or .xls)</li>
          <li><strong>Select multiple files</strong> - you can upload both debit and credit card statements at once</li>
          <li>The system will automatically:
            <ul className="ml-6 mt-1 space-y-1 list-disc">
              <li>Detect file format (Garanti Debit or Credit Card)</li>
              <li>Skip duplicate transactions across all files</li>
              <li>Import only new transactions</li>
            </ul>
          </li>
          <li>You can re-upload the same files multiple times - duplicates will always be skipped</li>
        </ol>
        
        <div className="mt-4 p-4 bg-primary/10 border border-primary/30 rounded-lg">
          <h4 className="text-sm font-semibold mb-2 flex items-center">
            <CheckCircle size={16} className="mr-2 text-success" />
            Supported Formats
          </h4>
          <ul className="text-sm text-text-muted space-y-1 ml-6 list-disc">
            <li>Garanti Bank Debit Account Statements (.xlsx, .xls)</li>
            <li>Garanti Bank Credit Card Statements (.xlsx, .xls)</li>
          </ul>
        </div>
      </div>
    </div>
  );
};

export default Import;

