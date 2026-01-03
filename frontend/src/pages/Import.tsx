import { useState } from 'react';
import { Upload, CheckCircle, XCircle, FileText, Loader, Trash2, AlertTriangle } from 'lucide-react';
import { importAPI } from '../utils/api';

const Import = () => {
  const [files, setFiles] = useState<File[]>([]);
  const [result, setResult] = useState<any>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [processingStatus, setProcessingStatus] = useState<{[key: string]: 'pending' | 'processing' | 'success' | 'error'}>({});
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [deleteResult, setDeleteResult] = useState<any>(null);
  
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

  const handleDeleteAll = async () => {
    setDeleting(true);
    setError(null);
    setDeleteResult(null);
    
    try {
      const response = await importAPI.deleteAllData();
      setDeleteResult(response.data);
      setShowDeleteConfirm(false);
      // Clear any import results
      setResult(null);
      setFiles([]);
      // Reload page after 2 seconds to refresh all data
      setTimeout(() => {
        window.location.reload();
      }, 2000);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to delete all data');
      setShowDeleteConfirm(false);
    } finally {
      setDeleting(false);
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
            multiple={true}
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
      
      {/* Delete All Data Section */}
      <div className="card border-l-4 border-danger">
        <div className="flex items-start justify-between mb-4">
          <div className="flex-1">
            <h3 className="text-xl font-semibold text-danger mb-2">Delete All Data</h3>
            <p className="text-text-muted text-sm">
              Permanently delete all transactions, accounts, budgets, rules, and insights from the database.
              <strong className="text-danger block mt-1">This action cannot be undone!</strong>
            </p>
          </div>
          <AlertTriangle size={24} className="text-danger ml-4" />
        </div>
        
        {!showDeleteConfirm && (
          <button
            onClick={() => setShowDeleteConfirm(true)}
            disabled={deleting}
            className="btn-danger"
          >
            <Trash2 size={18} className="mr-2" />
            Delete All Data
          </button>
        )}
        
        {showDeleteConfirm && (
          <div className="space-y-4">
            <div className="p-4 bg-danger/10 border border-danger/30 rounded-lg">
              <div className="flex items-start space-x-3">
                <AlertTriangle size={24} className="text-danger mt-0.5" />
                <div className="flex-1">
                  <h4 className="font-semibold text-danger mb-2">Are you absolutely sure?</h4>
                  <p className="text-sm text-text-muted">
                    This will permanently delete:
                  </p>
                  <ul className="text-sm text-text-muted list-disc list-inside mt-2 space-y-1">
                    <li>All transactions</li>
                    <li>All accounts</li>
                    <li>All budgets</li>
                    <li>All rules</li>
                    <li>All insights</li>
                  </ul>
                  <p className="text-sm font-semibold text-danger mt-3">
                    This action cannot be undone!
                  </p>
                </div>
              </div>
            </div>
            
            <div className="flex space-x-3">
              <button
                onClick={handleDeleteAll}
                disabled={deleting}
                className="btn-danger flex-1"
              >
                {deleting ? (
                  <>
                    <Loader size={18} className="mr-2 animate-spin" />
                    Deleting...
                  </>
                ) : (
                  <>
                    <Trash2 size={18} className="mr-2" />
                    Yes, Delete Everything
                  </>
                )}
              </button>
              <button
                onClick={() => setShowDeleteConfirm(false)}
                disabled={deleting}
                className="btn-secondary flex-1"
              >
                Cancel
              </button>
            </div>
          </div>
        )}
        
        {deleteResult && (
          <div className="mt-4 p-4 bg-background rounded-lg border border-gray-700">
            <div className="flex items-center space-x-2 mb-2">
              <CheckCircle className="text-success" size={20} />
              <h4 className="font-semibold">Data Deleted Successfully</h4>
            </div>
            <div className="grid grid-cols-2 md:grid-cols-5 gap-3 text-sm">
              <div>
                <p className="text-text-muted">Transactions</p>
                <p className="font-semibold">{deleteResult.transactionsDeleted || 0}</p>
              </div>
              <div>
                <p className="text-text-muted">Accounts</p>
                <p className="font-semibold">{deleteResult.accountsDeleted || 0}</p>
              </div>
              <div>
                <p className="text-text-muted">Insights</p>
                <p className="font-semibold">{deleteResult.insightsDeleted || 0}</p>
              </div>
              <div>
                <p className="text-text-muted">Budgets</p>
                <p className="font-semibold">{deleteResult.budgetsDeleted || 0}</p>
              </div>
              <div>
                <p className="text-text-muted">Rules</p>
                <p className="font-semibold">{deleteResult.rulesDeleted || 0}</p>
              </div>
            </div>
            <p className="text-xs text-text-muted mt-2">Page will refresh in a moment...</p>
          </div>
        )}
      </div>

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

