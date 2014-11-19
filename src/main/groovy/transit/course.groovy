package transit

import groovy.sql.Sql

class CreditsAreNaN extends Exception {
  String Credits
  CreditsAreNaN(String subject, String number, String credits) {
    this.Subject = subject
    this.Number = number
    this.Credits = credits
  }
  String toString() {
    return "The proposed credits, '${this.Credits}', aren't a number."
  }
}

class CourseAlreadyAdded extends Exception {
  String Subject
  String Number
  CourseAlreadyAdded(String subject, String number) {
    this.Subject = subject
    this.Number = number
  }
  String toString() {
    return "Course, '${this.Subject} ${this.Number}', has already been added."
  }
}

class Course {
  String Credits
  String Grade
  String Subject
  String Number
  String Title
  String TermCode
  String TrcrNo
  
  Course(String credits, String grade, String subject, String number, String title, 
         String termCode, Sql sql) {
    this.Credits = credits
    this.Grade = grade
    this.Subject = subject
    this.Number = number
    this.Title = title
    this.TermCode = termCode
  
    this.fixCredits()
    this.fixGrade(sql)
    this.fixStar()
    this.fixSubject()
    
    if(!this.Credits.isNumber()) {
      throw new CreditsAreNaN(this.Subject, this.Number, this.Credits)
    }
  }
    
  private def fixCredits() { 
    this.Credits = (this.Credits) ? this.Credits : 0
    if(this.Credits.contains(' ')) {
      this.Credits = (this.Credits.trim() =~ /\s+/).replaceAll('.')  
    }
    if(this.Credits.toDouble() > 99) {
      this.Credits = (this.Credits.toDouble() / 100).toString()
    }
  }
  
  private def fixGrade(sql) {
    if(!this.Grade) {
      this.Grade = (this.TermCode >= sql.firstRow("select wcu.getterm('c') curr_term from dual").curr_term) ?
                    'IP' : 'NG'
    }  
  }
  
  private def fixStar() {
    if (this.Number.contains('*')) {
      this.Subject = subject + ('*' * number.count('*'))
      this.Number = number.replaceAll(~/\*/, '')
    }
  }
  
  private def fixSubject() {
    if (this.Subject ==~ /(B|8)(I|1)(O|0).*/) {
      this.Subject = 'BIO' + this.Subject[3..(this.Subject.length()-1)]
    }  
  }

 def doesntExist(String pidm, String sbgi, Sql sql) {
    String query = '''select *
                        from shrtrcr
                        join shrtrit
                          on shrtrit_pidm = shrtrcr_pidm
                         and shrtrit_seq_no = shrtrcr_trit_seq_no
                         and shrtrit_sbgi_code = ?
                       where shrtrcr_pidm = ?
                         and shrtrcr_trans_course_name = ?
                         and shrtrcr_trans_course_numbers = ?
                         and shrtrcr_term_code = ?'''
    if (sql.firstRow(query, [sbgi, pidm, this.Subject, this.Number, this.TermCode]) == null) {
      return true
    } else {
      throw new CourseAlreadyAdded(this.Subject, this.Number) 
    } 
  }
  
  def setTitle(String sbgi, Sql sql) {
    String query = '''select SHBTATC_TRNS_TITLE
                        from SHBTATC
                       where SHBTATC_sbgi_code = ?
                         and shbtatc_subj_code_trns = ?
                         and shbtatc_crse_numb_trns = ?
                         and SHBTATC_term_code_eff_trns =
                             (select max(z.shbtatc_term_code_eff_trns)
                                from shbtatc z
                               where z.shbtatc_sbgi_code = shbtatc.shbtatc_sbgi_code
                                 and z.shbtatc_subj_code_trns = shbtatc.shbtatc_subj_code_trns
                                 and z.shbtatc_crse_numb_trns = shbtatc.shbtatc_crse_numb_trns
                                 and z.shbtatc_term_code_eff_trns <= ?)'''
    this.Title = (sql.firstRow(query, [sbgi, this.Subject, this.Number, this.TermCode])?.shbtatc_trns_title) ?
                  sql.firstRow(query, [sbgi, this.Subject, this.Number, this.TermCode]).shbtatc_trns_title :
                  this.Title.toUpperCase()    
  }
  
  def setTrcr(String pidm, String tritNo, String tramNo, Sql sql) {
    String qNextTrcr = '''select max(shrtrcr_seq_no) + 1 next_trcr
                            from shrtrcr
                           where shrtrcr_pidm = ?
                             and shrtrcr_trit_seq_no = ?
                             and shrtrcr_tram_seq_no = ?'''
    this.TrcrNo = (sql.firstRow(qNextTrcr, [pidm, tritNo, tramNo]).next_trcr) ?
                   sql.firstRow(qNextTrcr, [pidm, tritNo, tramNo]).next_trcr : 1
  }  
  
  def setCourse(String pidm, String tritNo, String tramNo, Sql sql) {
    String query = "call wcuregistrar.uas_k_arec_xfercredit_load.pInsertShrtrcr" +
                   "(${pidm}, ${tritNo}, ${tramNo}, ${this.TrcrNo}, '${this.Subject}', " +
                   "'${this.Number}', ${this.Credits}, '${this.Grade}', 'UG', " +
                   " ${this.TermCode}, '${this.Title}')"
    sql.execute(query)
  }
}