package transit

import groovy.sql.Sql

class Connection {
  Sql DB
  String IODir
  String Profile
  
  Connection() {
    String connString
    String username
    String password
    String driver
    
    def settingsXML = new XmlSlurper().parse('settings.xml')
    for(int i = 0; i < settingsXML.size(); i++) {
      if(settingsXML.profile[i].@status.text().toUpperCase() == 'ACTIVE')
       {
        connString = settingsXML.profile[i].connString.text()
        username = settingsXML.profile[i].username.text()
        password = settingsXML.profile[i].password.text()
        driver = settingsXML.profile[i].driver.text()
        
        this.DB = Sql.newInstance(connString, username, password, driver)
        this.IODir = settingsXML.profile[i]?.ioDir?.text() // IODir does not need to be defined
        this.Profile = settingsXML.profile[i].@name.text()
       }    
    }
  }
  def closeDB() {
    this.DB.close()
  }
}