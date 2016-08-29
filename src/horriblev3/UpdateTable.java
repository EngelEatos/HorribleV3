package horriblev3;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.beans.PropertyChangeEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.BevelBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import to.uploaded.Uploaded;
import to.uploaded.exception.DownloadFailedException;
import to.uploaded.file.UPDownload;

/**
 *
 * @author EngelEatos
 */
public class UpdateTable {

    public static void main(String[] args) {
        UpdateTable updateTable = new UpdateTable();
    }

    public UpdateTable() {
        EventQueue.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            }
            
            UpdatableTableModel model = new UpdatableTableModel();
            
            JTable table = new JTable();
            table.setModel((TableModel) model);
            
            table.getColumn("Status").setCellRenderer((TableCellRenderer) new ProgressCellRender());
            
            
            JFrame frame = new JFrame();
            frame.setSize(500,500);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());
            //set up statusbar
            JPanel statusPanel = new JPanel();
            statusPanel.setBorder(new BevelBorder(BevelBorder.LOWERED));
            frame.add(statusPanel, BorderLayout.SOUTH);
            statusPanel.setPreferredSize(new Dimension(frame.getWidth(), 16));
            statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.X_AXIS));
            JLabel statusLabel = new JLabel("status");
            statusLabel.setHorizontalAlignment(SwingConstants.LEFT);
            statusPanel.add(statusLabel);
            //
            frame.add(new JScrollPane(table));
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            List<Anime> animes = null;
            try {
                animes = new Collector().get();
            } catch (IOException | ParseException ex) {
                Logger.getLogger(UpdateTable.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            FileFinderWorker worker = new FileFinderWorker(animes, model);
            worker.execute();
        });
    }

    public class ProgressCellRender extends JProgressBar implements TableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            int progress = 0;
            if (value instanceof Float) {
                progress = Math.round(((Float) value) * 100f);
            } else if (value instanceof Integer) {
                progress = (int) value;
            }
            setValue(progress);
            return this;
        }
    }

    public class RowData {
        private String anime;
        private String episode;
        private float status;

        public RowData(String anime, String episode) {
            this.anime = anime;
            this.episode = episode;
            this.status = 0f;
        }

        public String getAnime(){
            return anime;
        }
        public String getEpisode() {
            return episode;
        }

        public float getStatus() {
            return status;
        }

        public void setStatus(float status) {
            this.status = status;
        }
    }

    public class UpdatableTableModel extends AbstractTableModel {

        private final List<RowData> rows;
        private final Map<Anime, RowData> mapLookup;

        public UpdatableTableModel() {
            rows = new ArrayList<>(25);
            mapLookup = new HashMap<>(25);
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public String getColumnName(int column) {
            String name = "??";
            switch (column) {
                case 0:
                    name = "Anime";
                    break;
                case 1:
                    name = "Episode";
                    break;
                case 2:
                    name = "Status";
                    break;
            }
            return name;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            RowData rowData = rows.get(rowIndex);
            Object value = null;
            switch (columnIndex) {
                case 0:
                    value = rowData.getAnime();
                    break;
                case 1:
                    value = rowData.getEpisode();
                    break;
                case 2:
                    value = rowData.getStatus();
                    break;
            }
            return value;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            RowData rowData = rows.get(rowIndex);
            switch (columnIndex) {
                case 2:
                    if (aValue instanceof Float) {
                        rowData.setStatus((float) aValue);
                    }
                    break;
            }
        }

        public void addFile(Anime anime) {
            RowData rowData = new RowData(anime.name, anime.ep);
            mapLookup.put(anime, rowData);
            rows.add(rowData);
            fireTableRowsInserted(rows.size() - 1, rows.size() - 1);
        }

        protected void updateStatus(Anime anime, int progress) {
            RowData rowData = mapLookup.get(anime);
            if (rowData != null) {
                int row = rows.indexOf(rowData);
                float p = (float) progress / 100f;
                setValueAt(p, row, 2);
                fireTableCellUpdated(row, 2);
            }
        }
    }

    public class FileFinderWorker extends SwingWorker<List<Anime>, Anime> {

        private final UpdatableTableModel model;
        private final List<Anime> animes;

        public FileFinderWorker(List<Anime> animes, UpdatableTableModel model) {
            this.model = model;
            this.animes = animes;
        }

        @Override
        protected void process(List<Anime> chunks) {
            chunks.stream().forEach((anime) -> {
                model.addFile(anime);
            });
        }

        @Override
        protected List<Anime> doInBackground() throws Exception {
            animes.stream().forEach((anime) -> {
                // You could actually publish the entire array, but I'm doing this
                // deliberatly ;)
                publish(anime);
            });
            return animes;
        }

        @Override
        protected void done() {
            try {
                List<Anime> files = get();
                files.stream().forEach((anime) -> {
                    new FileReaderWorker(model, anime).execute();
                });
            } catch (InterruptedException | ExecutionException exp) {
                exp.printStackTrace();
            }
        }
    }

    public class FileReaderWorker extends SwingWorker<File, File> {

        private Anime currentFile;
        private UpdatableTableModel model;

        public FileReaderWorker(UpdatableTableModel model, Anime anime) {
            this.currentFile = anime;
            this.model = model;

            addPropertyChangeListener((PropertyChangeEvent evt) -> {
                if (evt.getPropertyName().equals("progress")) {
                    FileReaderWorker.this.model.updateStatus(currentFile, (int) evt.getNewValue());
                }
            });

        }

        @Override
        protected File doInBackground() throws Exception {
            return null;
            
        }
    }
}
