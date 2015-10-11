package util

/**
 * Created with IntelliJ IDEA.
 * User: faissalboutaounte
 * Date: 15-01-28
 * Time: 23:29
 * To change this template use File | Settings | File Templates.
 */
object HTMLUtilities {
  def truncate(html:String,length:Int=1000)={
    if(html.length > 1000){
      html
    }else html.substring(0,1000)
  }

}
