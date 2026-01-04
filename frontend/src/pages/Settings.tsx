import { useState } from 'react';
import { Trash2, AlertTriangle, CheckCircle, Loader } from 'lucide-react';
import { settingsAPI } from '../utils/api';

const Settings = () => {
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [deleteResult, setDeleteResult] = useState<any>(null);
  const [error, setError] = useState<string | null>(null);

  const handleDeleteAll = async () => {
    setDeleting(true);
    setError(null);
    setDeleteResult(null);

    try {
      const response = await settingsAPI.deleteAllData();
      setDeleteResult(response.data);
      setShowDeleteConfirm(false);
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
      <h1 className="text-3xl font-bold">Settings</h1>

      {/* Delete All Data Section */}
      <div className="card">
        <div className="mb-6">
          <h2 className="text-xl font-semibold mb-2">Data Management</h2>
          <p className="text-text-muted text-sm">
            Manage your data and account settings
          </p>
        </div>

        <div className="border-t border-gray-700 pt-6">
          <div className="flex items-start justify-between mb-4">
            <div className="flex-1">
              <h3 className="text-lg font-semibold mb-2">Delete All Data</h3>
              <p className="text-text-muted text-sm mb-1">
                Permanently delete all transactions from the database.
              </p>
              <p className="text-text-muted text-xs">
                This action cannot be undone. All imported transaction data will be permanently removed.
              </p>
            </div>
          </div>

          {!showDeleteConfirm && !deleteResult && (
            <button
              onClick={() => setShowDeleteConfirm(true)}
              disabled={deleting}
              className="px-4 py-2 bg-danger hover:bg-danger/90 text-white rounded-lg font-medium transition-colors flex items-center space-x-2"
            >
              <Trash2 size={18} />
              <span>Delete All Data</span>
            </button>
          )}

          {showDeleteConfirm && !deleteResult && (
            <div className="space-y-4">
              <div className="p-4 bg-danger/10 border border-danger/30 rounded-lg">
                <div className="flex items-start space-x-3">
                  <AlertTriangle size={20} className="text-danger mt-0.5 flex-shrink-0" />
                  <div className="flex-1">
                    <h4 className="font-semibold text-danger mb-2">Are you absolutely sure?</h4>
                    <p className="text-sm text-text-muted mb-3">
                      This will permanently delete all transactions from your database. This action cannot be undone.
                    </p>
                    <div className="bg-background/50 p-3 rounded border border-gray-700">
                      <p className="text-sm font-medium text-text mb-1">What will be deleted:</p>
                      <ul className="text-sm text-text-muted list-disc list-inside space-y-0.5">
                        <li>All transaction records</li>
                        <li>All spending data and analytics</li>
                      </ul>
                    </div>
                  </div>
                </div>
              </div>

              <div className="flex space-x-3">
                <button
                  onClick={handleDeleteAll}
                  disabled={deleting}
                  className="px-4 py-2 bg-danger hover:bg-danger/90 text-white rounded-lg font-medium transition-colors flex items-center space-x-2 flex-1 justify-center"
                >
                  {deleting ? (
                    <>
                      <Loader size={18} className="animate-spin" />
                      <span>Deleting...</span>
                    </>
                  ) : (
                    <>
                      <Trash2 size={18} />
                      <span>Yes, Delete Everything</span>
                    </>
                  )}
                </button>
                <button
                  onClick={() => setShowDeleteConfirm(false)}
                  disabled={deleting}
                  className="px-4 py-2 bg-background hover:bg-gray-700 text-text border border-gray-600 rounded-lg font-medium transition-colors flex-1"
                >
                  Cancel
                </button>
              </div>
            </div>
          )}

          {error && (
            <div className="mt-4 p-4 bg-danger/10 border border-danger/30 rounded-lg">
              <div className="flex items-center space-x-2">
                <AlertTriangle className="text-danger" size={20} />
                <p className="text-sm text-danger">{error}</p>
              </div>
            </div>
          )}

          {deleteResult && (
            <div className="mt-4 p-4 bg-success/10 border border-success/30 rounded-lg">
              <div className="flex items-center space-x-2 mb-3">
                <CheckCircle className="text-success" size={20} />
                <h4 className="font-semibold text-success">Data Deleted Successfully</h4>
              </div>
              <div className="bg-background/50 p-3 rounded border border-gray-700">
                <p className="text-sm text-text-muted mb-2">Deleted records:</p>
                <p className="text-lg font-semibold text-text">
                  {deleteResult.transactionsDeleted || 0} transactions
                </p>
              </div>
              <p className="text-xs text-text-muted mt-3">
                The page will refresh shortly...
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default Settings;

