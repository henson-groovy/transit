package transit

import groovy.sql.Sql

class InvalidAttendancePeriod extends Exception {
  String AttendancePeriod
  InvalidAttendancePeriod(String attendancePeriod) {
    this.AttendancePeriod = attendancePeriod
  }
  String toString() {
    return "The Attendance Period, '${this.AttendancePeriod}', is invalid."
  }
}

class EmptyAttendancePeriod extends Exception {
   String toString() {
    return "The Attendance Period to be added has no courses."
  }
}

class EmptyAttendancePeriods extends Exception {
   String toString() {
    return "Transcript has no Attendance Periods."
  }
}

class AttendancePeriod {
  ArrayList Courses = []
  String TermCode
  String TermDesc
  String SBGICode
  String SBGIDesc
  String TritNo
  String TramNo
  
  AttendancePeriod(String termCode, String sbgiCode, Sql sql) {
    this.TermCode = termCode
    this.SBGICode = sbgiCode
    
    if(this.TermCode ==~ /\d{4}WINTER/) {
      this.TermCode = "${this.TermCode[0..3].toInteger() + 1}10"
    }
    if(this.TermCode ==~ /\d{6}/) {
      switch( this.TermCode[4,5] ) {
        case '80':
          this.TermDesc = "08/${this.TermCode[2,3]}-12/${this.TermCode[2,3]}"
          break
        case '50':
          this.TermDesc = "05/${this.TermCode[2,3]}-08/${this.TermCode[2,3]}"
          break
        case '10':
          this.TermDesc = "01/${this.TermCode[2,3]}-05/${this.TermCode[2,3]}"
          break
      }      
      this.SBGIDesc = sql.firstRow('select stvsbgi_desc from stvsbgi where stvsbgi_code = ?',
                                   [sbgiCode])?.stvsbgi_desc
    } else {
      throw new InvalidAttendancePeriod(this.TermCode)
    }   
  }
  
  def setTrit(String pidm, Sql sql) {
    String qTrit = '''select shrtrit_seq_no
                        from shrtrit
                       where shrtrit_pidm = ?
                         and shrtrit_sbgi_code = ?'''
    String qNextTrit = '''select max(shrtrit_seq_no) + 1 next_trit
                            from shrtrit
                           where shrtrit_pidm = ?'''
    this.TritNo = sql.firstRow(qTrit, [pidm, this.SBGICode])?.shrtrit_seq_no
    if (!this.TritNo) {
      this.TritNo = (sql.firstRow(qNextTrit, [pidm]).next_trit) ?
                     sql.firstRow(qNextTrit, [pidm]).next_trit : 1
      String query = "call wcuregistrar.uas_k_arec_xfercredit_load.pInsertShrtrit" +
                     "(${pidm}, ${this.TritNo}, '${this.SBGICode}', '${this.SBGIDesc}')"
      sql.execute(query)
    }
  }
  
  def setTram(String pidm, Sql sql) {
    String qTram = '''select shrtram_seq_no
                        from shrtram
                       where shrtram_pidm = ?
                         and shrtram_trit_seq_no = ?
                         and shrtram_term_code_entered = ?'''
    String qNextTram = '''select max(shrtram_seq_no) + 1 next_tram
                            from shrtram
                           where shrtram_pidm = ?
                             and shrtram_trit_seq_no = ?'''
    this.TramNo = sql.firstRow(qTram, [pidm, this.TritNo, this.TermCode])?.shrtram_seq_no
    if (!this.TramNo) {
      this.TramNo = (sql.firstRow(qNextTram, [pidm, this.TritNo]).next_tram) ?
                     sql.firstRow(qNextTram, [pidm, this.TritNo]).next_tram : 1
      String query = "call wcuregistrar.uas_k_arec_xfercredit_load.pInsertShrtram" +
                     "(${pidm}, ${this.TritNo}, ${this.TramNo}, 'UG', '${this.TermDesc}', '${this.TermCode}')"
      sql.execute(query)
    }
  } 
}