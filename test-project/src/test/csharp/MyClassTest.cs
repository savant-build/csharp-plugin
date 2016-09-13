using NUnit.Framework;
using NLog;

namespace Org.SavantBuild.Test
{
  [TestFixture]
  public class MyClass
  {
    protected static readonly Logger logger = LogManager.GetCurrentClassLogger();
  }
}