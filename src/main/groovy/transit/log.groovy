package transit

class Log {
  BufferedWriter ErrLog
  BufferedWriter OKLog
  String Prefix
  boolean debug = true
  
  Log(String baseLocation) {
    this.ErrLog = new File(baseLocation + '\\error_log.txt').newWriter(true)
    this.OKLog  = new File(baseLocation + '\\log.txt').newWriter(true)
  }
  def setPrefix(String prefix) {
    if(prefix) {
      this.Prefix = prefix + ': '
    }
  }
  def logIt(String message, Boolean isError=false) {
    if(debug) {
      println this.Prefix + message
    }
    if(isError) {
      this.ErrLog.write(this.Prefix + message + "\r\n") 
    } else {
      this.OKLog.write(this.Prefix + message + "\r\n")
    }
  }
  def closeLog() {
    this.ErrLog.close()
    this.OKLog.close()  
  }
}