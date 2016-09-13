using System.Web;
using NLog;

namespace Org.SavantBuild.Test
{
  public class MyClass
  {
    protected static readonly Logger logger = LogManager.GetCurrentClassLogger();

    protected HttpWebRequest request;
  }
}