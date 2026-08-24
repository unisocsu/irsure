private List<IrLoader.Protocol> protocols;
private int currentIndex = 0;

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    // טעינת כל 200+ הקודים מה-XML בבת אחת
    protocols = IrLoader.loadCodesFromXml(this);
    
    // ... שאר הקוד של הכפתורים ...
}

private void runScanStep() {
    if (!isScanning || currentIndex >= protocols.size()) {
        stopAutoScan();
        return;
    }

    IrLoader.Protocol p = protocols.get(currentIndex);
    statusText.setText("בודק: " + p.name + " (" + (currentIndex + 1) + "/" + protocols.size() + ")");
    
    // שליחה פיזית
    irManager.transmit(38000, p.pattern);
    
    currentIndex++;
    scanHandler.postDelayed(this::runScanStep, 2000);
}
